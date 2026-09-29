package com.example.vitamin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class UserFragment : Fragment() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    private val geminiModels = arrayOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash-exp",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "Kustom"
    )

    private val openRouterModels = arrayOf(
        "google/gemini-2.5-flash",
        "meta-llama/llama-3.3-70b-instruct:free",
        "deepseek/deepseek-r1:free",
        "openai/gpt-4o-mini",
        "anthropic/claude-3.5-haiku",
        "Kustom"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = VitaminDbHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        val userId = sessionManager.getUserId()
        val user = dbHelper.getUser(userId)

        val tvNickname = view.findViewById<TextView>(R.id.tv_user_nickname)
        val tvHandle = view.findViewById<TextView>(R.id.tv_username_handle)
        val etNewNickname = view.findViewById<EditText>(R.id.et_new_nickname)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update_nickname)
        val btnLogout = view.findViewById<Button>(R.id.btn_user_logout)
        val btnDelete = view.findViewById<Button>(R.id.btn_user_delete_account)

        // Gender UI
        val spinnerGender = view.findViewById<Spinner>(R.id.spinner_user_gender)
        val btnUpdateGender = view.findViewById<Button>(R.id.btn_update_gender)

        val genders = arrayOf("Perempuan", "Laki-laki")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        val currentGender = user?.gender ?: "Perempuan"
        val genderIndex = if (currentGender == "Laki-laki") 1 else 0
        spinnerGender.setSelection(genderIndex)

        btnUpdateGender.setOnClickListener {
            val selectedGender = genders[spinnerGender.selectedItemPosition]
            dbHelper.updateGender(userId, selectedGender)
            Toast.makeText(requireContext(), "Jenis kelamin diperbarui menjadi $selectedGender!", Toast.LENGTH_SHORT).show()
        }

        // AI components
        val spinnerProvider = view.findViewById<Spinner>(R.id.spinner_user_ai_provider)
        val etApiKey = view.findViewById<EditText>(R.id.et_user_ai_api_key)
        val spinnerModel = view.findViewById<Spinner>(R.id.spinner_user_ai_model)
        val etModel = view.findViewById<EditText>(R.id.et_user_ai_model)
        val btnSaveAi = view.findViewById<Button>(R.id.btn_save_user_ai_key)

        if (user != null) {
            tvNickname.text = user.nickname
            tvHandle.text = "@${user.username} (${user.gender})"
        }

        // Hide or configure non-applicable options for single offline user mode
        btnLogout.visibility = View.GONE
        btnDelete.text = "Reset Semua Data Riwayat"

        btnUpdate.setOnClickListener {
            val newNickname = etNewNickname.text.toString()
            if (newNickname.isEmpty()) {
                Toast.makeText(requireContext(), "Nama panggilan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.updateNickname(userId, newNickname)
            tvNickname.text = newNickname
            etNewNickname.text.clear()
            Toast.makeText(requireContext(), "Nama panggilan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        }

        // Setup AI Provider & Model UI
        val providers = arrayOf("Google Gemini", "OpenRouter")
        val providerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, providers)
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProvider.adapter = providerAdapter

        val currentProvider = sessionManager.getProvider()
        val currentApiKey = sessionManager.getApiKey()
        val currentModel = sessionManager.getModel()

        etApiKey.setText(currentApiKey)
        etModel.setText(currentModel)

        val providerIndex = if (currentProvider == "OpenRouter") 1 else 0
        spinnerProvider.setSelection(providerIndex)

        fun updateModelSpinner(provider: String) {
            val models = if (provider == "OpenRouter") openRouterModels else geminiModels
            val modelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, models)
            modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerModel.adapter = modelAdapter

            val foundIndex = models.indexOfFirst { it == etModel.text.toString().trim() }
            if (foundIndex >= 0) {
                spinnerModel.setSelection(foundIndex)
            } else if (etModel.text.toString().isNotEmpty()) {
                spinnerModel.setSelection(models.size - 1) // Kustom
            } else {
                spinnerModel.setSelection(0)
                etModel.setText(models[0])
            }
        }

        updateModelSpinner(currentProvider)

        spinnerProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val selectedProvider = providers[position]
                updateModelSpinner(selectedProvider)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val selectedProvider = providers[spinnerProvider.selectedItemPosition]
                val models = if (selectedProvider == "OpenRouter") openRouterModels else geminiModels
                val selectedItem = models[position]
                if (selectedItem != "Kustom") {
                    etModel.setText(selectedItem)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSaveAi.setOnClickListener {
            val selectedProvider = providers[spinnerProvider.selectedItemPosition]
            val apiKeyText = etApiKey.text.toString().trim()
            var modelText = etModel.text.toString().trim()

            if (modelText.isEmpty()) {
                modelText = if (selectedProvider == "OpenRouter") "google/gemini-2.5-flash" else "gemini-2.5-flash"
                etModel.setText(modelText)
            }

            sessionManager.saveProvider(selectedProvider)
            sessionManager.saveApiKey(apiKeyText)
            sessionManager.saveModel(modelText)

            Toast.makeText(requireContext(), "Pengaturan AI ($selectedProvider - $modelText) berhasil disimpan!", Toast.LENGTH_SHORT).show()
        }

        btnDelete.setOnClickListener {
            val db = dbHelper.writableDatabase
            db.delete("biometrics", "user_id=?", arrayOf(userId.toString()))
            db.delete("calories", "user_id=?", arrayOf(userId.toString()))
            db.delete("menstrual", "user_id=?", arrayOf(userId.toString()))

            Toast.makeText(requireContext(), "Semua riwayat data berhasil di-reset!", Toast.LENGTH_SHORT).show()
        }
    }
}
