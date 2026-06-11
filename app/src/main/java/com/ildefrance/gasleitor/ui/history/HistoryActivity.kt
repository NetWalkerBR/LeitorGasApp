package com.ildefrance.gasleitor.ui.history

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ildefrance.gasleitor.R
import com.ildefrance.gasleitor.data.model.ReadingCycle
import com.ildefrance.gasleitor.data.repository.ReadingRepository
import com.ildefrance.gasleitor.databinding.ActivityHistoryBinding
import com.ildefrance.gasleitor.util.ExcelExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repository: ReadingRepository
    private var adapter: HistoryCycleAdapter? = null

    private val monthNames = listOf(
        "", "Janeiro", "Fevereiro", "Marco", "Abril", "Maio",
        "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ReadingRepository(this)
        binding.btnBack.setOnClickListener { finish() }

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val cycles = repository.getAllCycles()
            if (cycles.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE

                adapter = HistoryCycleAdapter(
                    cycles.toMutableList(),
                    monthNames,
                    onCardClick = { cycle -> showOptionsDialog(cycle) },
                    onDeleteClick = { cycle -> confirmDelete(cycle) }
                )
                binding.rvHistory.layoutManager = LinearLayoutManager(this@HistoryActivity)
                binding.rvHistory.adapter = adapter
            }
        }
    }

    // ---- Options dialog: Ver detalhes / Exportar Excel / Apagar -------------

    private fun showOptionsDialog(cycle: ReadingCycle) {
        val monthName = monthNames[cycle.month]
        val title = "$monthName / ${cycle.year}"
        val options = arrayOf("Ver detalhes", "Exportar e compartilhar Excel", "Apagar este registro")

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDetailDialog(cycle)
                    1 -> exportCycle(cycle)
                    2 -> confirmDelete(cycle)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---- Detail dialog -------------------------------------------------------

    private fun showDetailDialog(cycle: ReadingCycle) {
        lifecycleScope.launch {
            val readings = repository.getAllReadings(cycle.id)
            val count = readings.size
            val total = 44
            val monthName = monthNames[cycle.month]
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            val startStr = sdf.format(Date(cycle.startedAt))
            val finishStr = cycle.finishedAt?.let { sdf.format(Date(it)) } ?: "Em andamento"

            val msg = buildString {
                appendLine("Competencia: $monthName/${cycle.year}")
                appendLine("Iniciado: $startStr")
                appendLine("Finalizado: $finishStr")
                appendLine()
                appendLine("Leituras: $count / $total aptos")
                if (readings.isNotEmpty()) {
                    val avg = readings.sumOf { it.value } / readings.size
                    val min = readings.minOf { it.value }
                    val max = readings.maxOf { it.value }
                    val minApt = readings.minByOrNull { it.value }?.apartment ?: "-"
                    val maxApt = readings.maxByOrNull { it.value }?.apartment ?: "-"
                    appendLine(String.format(Locale("pt", "BR"), "Media: %.3f m3", avg))
                    appendLine(String.format(Locale("pt", "BR"), "Minimo: %.3f m3 (Apto $minApt)", min))
                    appendLine(String.format(Locale("pt", "BR"), "Maximo: %.3f m3 (Apto $maxApt)", max))
                }
            }

            AlertDialog.Builder(this@HistoryActivity)
                .setTitle("$monthName / ${cycle.year}")
                .setMessage(msg)
                .setPositiveButton("Exportar Excel") { _, _ -> exportCycle(cycle) }
                .setNegativeButton("Fechar", null)
                .show()
        }
    }

    // ---- Export + native share sheet ----------------------------------------

    private fun exportCycle(cycle: ReadingCycle) {
        lifecycleScope.launch {
            try {
                val readings = repository.getAllReadings(cycle.id)
                val file = ExcelExporter.export(this@HistoryActivity, cycle, readings)
                // Uses Intent.ACTION_SEND — Android shows WhatsApp, Drive, email, etc.
                ExcelExporter.shareFile(this@HistoryActivity, file)
            } catch (e: Exception) {
                AlertDialog.Builder(this@HistoryActivity)
                    .setTitle("Erro ao exportar")
                    .setMessage(e.message ?: "Erro desconhecido")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    // ---- Delete with confirmation --------------------------------------------

    private fun confirmDelete(cycle: ReadingCycle) {
        val monthName = monthNames[cycle.month]
        AlertDialog.Builder(this)
            .setTitle("Apagar registro?")
            .setMessage(
                "Isso vai apagar permanentemente a leitura de $monthName/${cycle.year} " +
                "e todos os dados associados.\n\nEsta acao nao pode ser desfeita."
            )
            .setPositiveButton("Apagar") { _, _ ->
                deleteCycle(cycle)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCycle(cycle: ReadingCycle) {
        lifecycleScope.launch {
            repository.deleteCycle(cycle.id)
            adapter?.removeCycle(cycle)

            // If list is now empty, show empty state
            if (adapter?.itemCount == 0) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            }
        }
    }
}

// ---- Adapter ----------------------------------------------------------------

class HistoryCycleAdapter(
    private val cycles: MutableList<ReadingCycle>,
    private val monthNames: List<String>,
    private val onCardClick: (ReadingCycle) -> Unit,
    private val onDeleteClick: (ReadingCycle) -> Unit
) : RecyclerView.Adapter<HistoryCycleAdapter.ViewHolder>() {

    fun removeCycle(cycle: ReadingCycle) {
        val idx = cycles.indexOfFirst { it.id == cycle.id }
        if (idx >= 0) {
            cycles.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_cycle, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cycle = cycles[position]
        holder.bind(cycle)
        holder.itemView.setOnClickListener { onCardClick(cycle) }
        holder.btnDelete.setOnClickListener { onDeleteClick(cycle) }
    }

    override fun getItemCount() = cycles.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_cycle)
        private val card: CardView = view.findViewById(R.id.card_cycle)
        private val tvMonth: TextView = view.findViewById(R.id.tv_cycle_month)
        private val tvYear: TextView = view.findViewById(R.id.tv_cycle_year)
        private val tvStatus: TextView = view.findViewById(R.id.tv_cycle_status)
        private val tvDate: TextView = view.findViewById(R.id.tv_cycle_date)
        private val tvCount: TextView = view.findViewById(R.id.tv_cycle_count)

        fun bind(cycle: ReadingCycle) {
            val ctx = itemView.context
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

            tvMonth.text = monthNames[cycle.month].uppercase()
            tvYear.text = cycle.year.toString()
            tvDate.text = sdf.format(Date(cycle.startedAt))

            if (cycle.finishedAt != null) {
                tvStatus.text = "Concluido"
                tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.green_done))
                card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.bg_done))
                tvCount.text = "44 / 44 lidos"
                tvCount.setTextColor(ContextCompat.getColor(ctx, R.color.green_done))
            } else {
                tvStatus.text = "Em andamento"
                tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.orange_pending))
                card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
                tvCount.text = "Incompleto"
                tvCount.setTextColor(ContextCompat.getColor(ctx, R.color.orange_pending))
            }
        }
    }
}
