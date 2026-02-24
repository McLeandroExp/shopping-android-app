package com.vishalgaur.shoppingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.vishalgaur.shoppingapp.data.utils.getISOCountriesMap
import com.vishalgaur.shoppingapp.databinding.FragmentAdminSettingsBinding
import com.vishalgaur.shoppingapp.viewModels.OrderViewModel
import java.util.*

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

        countryRulesAdapter = CountryRulesAdapter(
            emptyList(),
            onEdit = { code, config -> showAddRuleDialog(code, config) },
            onDelete = { code ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar Regla")
                    .setMessage("¿Deseas eliminar la regla para $code?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        currentCountryRules.remove(code)
                        updateRulesList()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
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

    private fun showAddRuleDialog(initialCode: String? = null, initialConfig: CountryConfig? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_country_rule, null)
        val countrySelectorTil = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.country_selector_til)
        val countrySelector = dialogView.findViewById<AutoCompleteTextView>(R.id.country_code_et)
        val shippingEt = dialogView.findViewById<EditText>(R.id.rule_shipping_et)
        val importEt = dialogView.findViewById<EditText>(R.id.rule_import_et)
        val taxEt = dialogView.findViewById<EditText>(R.id.rule_tax_et)

        val isoCountriesMap = getISOCountriesMap()
        val countries = isoCountriesMap.values.toSortedSet().toList()
        val countryAdapter = ArrayAdapter(requireContext(), R.layout.country_list_item, countries)
        
        countrySelector.setAdapter(countryAdapter)
        
        if (initialCode != null && initialConfig != null) {
            val countryName = isoCountriesMap[initialCode]
            countrySelectorTil.visibility = View.GONE
            countrySelector.setText(countryName, false)
            shippingEt.setText(initialConfig.shippingCharge.toString())
            importEt.setText(initialConfig.importCharge.toString())
            taxEt.setText(initialConfig.taxPercentage.toString())
        } else {
            countrySelectorTil.visibility = View.VISIBLE
            countrySelector.setText("Ecuador", false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (initialCode == null) "Agregar Regla por Pais" else "Editar Regla de ${initialCode}")
            .setView(dialogView)
            .setPositiveButton(if (initialCode == null) "Agregar" else "Actualizar") { _, _ ->
                val selectedName = countrySelector.text.toString()
                val code = isoCountriesMap.keys.find { isoCountriesMap[it] == selectedName }
                if (code != null) {
                    val config = CountryConfig(
                        shippingCharge = shippingEt.text.toString().toDoubleOrNull() ?: 0.0,
                        importCharge = importEt.text.toString().toDoubleOrNull() ?: 0.0,
                        taxPercentage = taxEt.text.toString().toDoubleOrNull() ?: 0.0
                    )
                    // If editing and code changed, remove old one
                    if (initialCode != null && initialCode != code) {
                        currentCountryRules.remove(initialCode)
                    }
                    currentCountryRules[code] = config
                    updateRulesList()
                } else {
                    Toast.makeText(requireContext(), "Error al identificar el país", Toast.LENGTH_SHORT).show()
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
        private val onEdit: (String, CountryConfig) -> Unit,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<CountryRulesAdapter.ViewHolder>() {

        private val isoCountriesMap = getISOCountriesMap()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val countryNameTv = view.findViewById<android.widget.TextView>(R.id.rule_country_name_tv)
            val detailsTv = view.findViewById<android.widget.TextView>(R.id.rule_details_tv)
            val editBtn = view.findViewById<android.widget.ImageButton>(R.id.rule_edit_btn)
            val deleteBtn = view.findViewById<android.widget.ImageButton>(R.id.rule_delete_btn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country_rule, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (code, config) = data[position]
            val countryName = isoCountriesMap[code] ?: code
            holder.countryNameTv.text = "$countryName ($code)"
            holder.detailsTv.text = "Envío: ${config.shippingCharge}, Import.: ${config.importCharge}, Impuesto: ${config.taxPercentage}%"
            
            holder.editBtn.setOnClickListener { onEdit(code, config) }
            holder.deleteBtn.setOnClickListener { onDelete(code) }
        }

        override fun getItemCount() = data.size

        fun updateData(newData: List<Pair<String, CountryConfig>>) {
            data = newData
            notifyDataSetChanged()
        }
    }
}
