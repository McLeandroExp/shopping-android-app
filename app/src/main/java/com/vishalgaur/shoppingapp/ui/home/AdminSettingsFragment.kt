package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vishalgaur.shoppingapp.R
import com.vishalgaur.shoppingapp.data.AppConfig
import com.vishalgaur.shoppingapp.data.CountryConfig
import com.vishalgaur.shoppingapp.databinding.FragmentAdminSettingsBinding
import com.vishalgaur.shoppingapp.viewModels.OrderViewModel

class AdminSettingsFragment : Fragment() {

    private lateinit var binding: FragmentAdminSettingsBinding
    private val viewModel: OrderViewModel by activityViewModels()
    private lateinit var countryRulesAdapter: CountryRulesAdapter
    private var currentCountryRules = mutableMapOf<String, CountryConfig>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        setupUI()
        observeViewModel()
        return binding.root
    }

    private fun setupUI() {
        binding.adminSettingsTopAppBar.topAppBar.title = "Configuracion de Cargos"
        binding.adminSettingsTopAppBar.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        countryRulesAdapter = CountryRulesAdapter(emptyList()) { countryCode ->
            currentCountryRules.remove(countryCode)
            updateRulesList()
        }
        binding.countryRulesRv.layoutManager = LinearLayoutManager(requireContext())
        binding.countryRulesRv.adapter = countryRulesAdapter

        binding.addCountryRuleBtn.setOnClickListener {
            showAddRuleDialog()
        }

        binding.saveSettingsBtn.setOnClickListener {
            saveSettings()
        }
    }

    private fun observeViewModel() {
        viewModel.appConfig.observe(viewLifecycleOwner) { config ->
            if (config != null) {
                binding.defaultShippingEt.setText(config.defaultConfig.shippingCharge.toString())
                binding.defaultImportEt.setText(config.defaultConfig.importCharge.toString())
                binding.defaultTaxEt.setText(config.defaultConfig.taxPercentage.toString())
                currentCountryRules = config.countryRules.toMutableMap()
                updateRulesList()
            }
        }
    }

    private fun updateRulesList() {
        countryRulesAdapter.updateData(currentCountryRules.toList())
    }

    private fun showAddRuleDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_country_rule, null)
        val countryEt = dialogView.findViewById<EditText>(R.id.country_code_et)
        val shippingEt = dialogView.findViewById<EditText>(R.id.rule_shipping_et)
        val importEt = dialogView.findViewById<EditText>(R.id.rule_import_et)
        val taxEt = dialogView.findViewById<EditText>(R.id.rule_tax_et)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar Regla por Pais")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val code = countryEt.text.toString().toUpperCase(java.util.Locale.ROOT)
                if (code.isNotEmpty()) {
                    val config = CountryConfig(
                        shippingCharge = shippingEt.text.toString().toDoubleOrNull() ?: 0.0,
                        importCharge = importEt.text.toString().toDoubleOrNull() ?: 0.0,
                        taxPercentage = taxEt.text.toString().toDoubleOrNull() ?: 0.0
                    )
                    currentCountryRules[code] = config
                    updateRulesList()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveSettings() {
        val defaultConfig = CountryConfig(
            shippingCharge = binding.defaultShippingEt.text.toString().toDoubleOrNull() ?: 0.0,
            importCharge = binding.defaultImportEt.text.toString().toDoubleOrNull() ?: 0.0,
            taxPercentage = binding.defaultTaxEt.text.toString().toDoubleOrNull() ?: 0.0
        )
        val newConfig = AppConfig(defaultConfig, currentCountryRules)
        viewModel.saveAppConfig(newConfig)
        Toast.makeText(requireContext(), "Configuracion guardada", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    inner class CountryRulesAdapter(
        private var data: List<Pair<String, CountryConfig>>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<CountryRulesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1 = view.findViewById<android.widget.TextView>(android.R.id.text1)
            val text2 = view.findViewById<android.widget.TextView>(android.R.id.text2)
            
            init {
                view.setOnLongClickListener {
                    val code = data[adapterPosition].first
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar Regla")
                        .setMessage("¿Deseas eliminar la regla para $code?")
                        .setPositiveButton("Eliminar") { _, _ -> onDelete(code) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (code, config) = data[position]
            holder.text1.text = "Pais: $code"
            holder.text2.text = "Envio: ${config.shippingCharge}, Import.: ${config.importCharge}, Impuesto: ${config.taxPercentage}%"
        }

        override fun getItemCount() = data.size

        fun updateData(newData: List<Pair<String, CountryConfig>>) {
            data = newData
            notifyDataSetChanged()
        }
    }
}
