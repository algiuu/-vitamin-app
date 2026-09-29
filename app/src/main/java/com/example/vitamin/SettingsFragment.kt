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
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: VitaminDbHelper

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
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = VitaminDbHelper(requireContext())

        val userId = sessionManager.getUserId()
        val user = dbHelper.getUser(userId)

        // Gender UI in Settings
        val spinnerGender = view.findViewById<Spinner>(R.id.spinner_setting_gender)
        val btnSaveGender = view.findViewById<Button>(R.id.btn_save_setting_gender)

        val genders = arrayOf("Perempuan", "Laki-laki")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        val currentGender = user?.gender ?: "Perempuan"
        val genderIndex = if (currentGender == "Laki-laki") 1 else 0
        spinnerGender.setSelection(genderIndex)

        btnSaveGender.setOnClickListener {
            val selectedGender = genders[spinnerGender.selectedItemPosition]
            dbHelper.updateGender(userId, selectedGender)
            Toast.makeText(requireContext(), "Jenis kelamin berhasil diperbarui menjadi $selectedGender!", Toast.LENGTH_SHORT).show()
        }

        // AI components
        val spinnerProvider = view.findViewById<Spinner>(R.id.spinner_ai_provider)
        val etApiKey = view.findViewById<EditText>(R.id.et_ai_api_key)
        val spinnerModel = view.findViewById<Spinner>(R.id.spinner_ai_model)
        val etModel = view.findViewById<EditText>(R.id.et_ai_model)
        val btnSave = view.findViewById<Button>(R.id.btn_save_ai_settings)

        // Setup Provider Spinner
        val providers = arrayOf("Google Gemini", "OpenRouter")
        val providerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, providers)
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProvider.adapter = providerAdapter

        // Load current saved values
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

            // Select matching model if present
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

        btnSave.setOnClickListener {
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
    }
}
