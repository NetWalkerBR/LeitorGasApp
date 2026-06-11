package com.ildefrance.gasleitor.ui.date

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ildefrance.gasleitor.data.repository.ReadingRepository
import com.ildefrance.gasleitor.databinding.ActivityDateConfirmBinding
import com.ildefrance.gasleitor.ui.history.HistoryActivity
import com.ildefrance.gasleitor.ui.reading.ReadingActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DateConfirmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDateConfirmBinding
    private lateinit var repository: ReadingRepository

    private var month: Int = 1
    private var year: Int = 2024

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ReadingRepository(this)

        val calendar = Calendar.getInstance()
        val locale = Locale("pt", "BR")

        val dayOfWeek = SimpleDateFormat("EEEE", locale).format(calendar.time)
            .replaceFirstChar { it.uppercase() }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val monthName = SimpleDateFormat("MMMM", locale).format(calendar.time)
            .replaceFirstChar { it.uppercase() }
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH) + 1

        binding.tvDayOfWeek.text = dayOfWeek
        binding.tvDay.text = day.toString()
        binding.tvMonthYear.text = "$monthName $year"

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnExit.setOnClickListener {
            showExitDialog()
        }
    }

    override fun onBackPressed() {
        showExitDialog()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sair do aplicativo?")
            .setMessage("Todo o progresso salvo até aqui será mantido.\nVocê pode continuar a leitura depois.")
            .setPositiveButton("Sair") { _, _ ->
                finishAffinity()  // closes app completely
            }
            .setNegativeButton("Continuar", null)
            .show()
    }

    // onResume runs every time the screen comes back into focus,
    // including after returning from HistoryActivity (where cycles may have been deleted).
    // This guarantees the open-cycle check is always fresh.
    override fun onResume() {
        super.onResume()
        refreshCycleState()
    }

    private fun refreshCycleState() {
        val calendar = Calendar.getInstance()
        val locale = Locale("pt", "BR")
        val monthName = SimpleDateFormat("MMMM", locale).format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        lifecycleScope.launch {
            val openCycle = repository.getOpenCycleForMonth(month, year)

            if (openCycle != null) {
                val done = repository.getCompletedCount(openCycle.id)
                binding.tvResumeBanner.text =
                    "Leitura de $monthName em andamento — $done/44 lidos"
                binding.tvResumeBanner.visibility = View.VISIBLE
                binding.btnConfirm.text = "Retomar Leitura"
            } else {
                binding.tvResumeBanner.visibility = View.GONE
                binding.btnConfirm.text = "Confirmar e Iniciar Leitura"
            }

            // Set click listener with the freshly queried openCycle
            binding.btnConfirm.setOnClickListener {
                lifecycleScope.launch {
                    val cycleId = openCycle?.id ?: repository.startCycle(month, year)
                    val intent = Intent(this@DateConfirmActivity, ReadingActivity::class.java).apply {
                        putExtra(ReadingActivity.EXTRA_CYCLE_ID, cycleId)
                        putExtra(ReadingActivity.EXTRA_MONTH, month)
                        putExtra(ReadingActivity.EXTRA_YEAR, year)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
