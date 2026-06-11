package com.ildefrance.gasleitor.ui.summary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ildefrance.gasleitor.data.repository.ReadingRepository
import com.ildefrance.gasleitor.databinding.ActivitySummaryBinding
import com.ildefrance.gasleitor.ui.splash.SplashActivity
import com.ildefrance.gasleitor.util.ExcelExporter
import kotlinx.coroutines.launch
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CYCLE_ID = "extra_cycle_id"
        const val EXTRA_MONTH = "extra_month"
        const val EXTRA_YEAR = "extra_year"
    }

    private lateinit var binding: ActivitySummaryBinding
    private lateinit var repository: ReadingRepository
    private var cycleId: Long = -1
    private var month: Int = 1
    private var year: Int = 2024

    private val monthNames = listOf("", "Janeiro", "Fevereiro", "Março", "Abril", "Maio",
        "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cycleId = intent.getLongExtra(EXTRA_CYCLE_ID, -1)
        month = intent.getIntExtra(EXTRA_MONTH, 1)
        year = intent.getIntExtra(EXTRA_YEAR, 2024)

        repository = ReadingRepository(this)

        binding.tvCompetence.text = "Competência: ${monthNames[month]}/$year"
        binding.tvSuccessMsg.text = "Leitura concluída com sucesso!"

        loadSummary()

        binding.btnExport.setOnClickListener { exportToExcel() }
        binding.btnNewCycle.setOnClickListener {
            startActivity(Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            val readings = repository.getAllReadings(cycleId)
            val total = 44
            val done = readings.size

            binding.tvReadingsCount.text = "$done / $total apartamentos lidos"
            binding.progressSummary.progress = done
            binding.progressSummary.max = total

            if (done > 0) {
                val avg = readings.sumOf { it.value } / done
                val min = readings.minOf { it.value }
                val max = readings.maxOf { it.value }
                val minApt = readings.minByOrNull { it.value }?.apartment ?: ""
                val maxApt = readings.maxByOrNull { it.value }?.apartment ?: ""

                binding.tvAvg.text = String.format(Locale("pt", "BR"), "Média: %.3f m³", avg)
                binding.tvMin.text = String.format(Locale("pt", "BR"), "Mínimo: %.3f m³ (Apto $minApt)", min)
                binding.tvMax.text = String.format(Locale("pt", "BR"), "Máximo: %.3f m³ (Apto $maxApt)", max)
                binding.statsGroup.visibility = View.VISIBLE
            }
        }
    }

    private fun exportToExcel() {
        lifecycleScope.launch {
            try {
                binding.btnExport.isEnabled = false
                binding.btnExport.text = "Gerando planilha…"

                val cycle = repository.getLatestCycle() ?: return@launch
                val readings = repository.getAllReadings(cycleId)
                val file = ExcelExporter.export(this@SummaryActivity, cycle, readings)

                binding.btnExport.isEnabled = true
                binding.btnExport.text = "Exportar Planilha Excel"

                ExcelExporter.shareFile(this@SummaryActivity, file)

            } catch (e: Exception) {
                binding.btnExport.isEnabled = true
                binding.btnExport.text = "Exportar Planilha Excel"
                Toast.makeText(this@SummaryActivity,
                    "Erro ao gerar planilha: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onBackPressed() {
        // Do not allow back navigation from summary — go to home
        startActivity(Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
