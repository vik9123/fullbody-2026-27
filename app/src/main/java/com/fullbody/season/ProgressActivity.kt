package com.fullbody.season

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fullbody.season.data.LogStore
import com.fullbody.season.ui.ChartView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProgressActivity : AppCompatActivity() {

    private lateinit var chart: ChartView
    private lateinit var historyText: TextView
    private lateinit var emptyText: TextView
    private lateinit var spinner: Spinner
    private var names: List<String> = emptyList()

    private val inFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val outFmt = DateTimeFormatter.ofPattern("dd.MM")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        chart = findViewById(R.id.chart)
        historyText = findViewById(R.id.historyText)
        emptyText = findViewById(R.id.emptyText)
        spinner = findViewById(R.id.exerciseSpinner)

        names = LogStore.loggedExerciseNames(this)
        if (names.isEmpty()) {
            spinner.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.text = "Записей пока нет. Проведите первую тренировку и отметьте подходы — графики появятся здесь."
            return
        }

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                render(names[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun render(exercise: String) {
        val history = LogStore.weightHistory(this, exercise)
        if (history.isEmpty()) {
            chart.points = emptyList()
            emptyText.visibility = View.VISIBLE
            emptyText.text = "По этому упражнению нет записей с весом."
            historyText.text = ""
            return
        }
        emptyText.visibility = View.GONE
        chart.points = history.map { (date, w) -> formatDate(date) to w }

        val sb = StringBuilder()
        for ((date, w) in history.asReversed().take(12)) {
            sb.append(formatDate(date)).append(" — макс. ").append(formatWeight(w)).append('\n')
        }
        historyText.text = sb.toString().trimEnd()
    }

    private fun formatDate(iso: String): String = try {
        LocalDate.parse(iso, inFmt).format(outFmt)
    } catch (e: Exception) {
        iso
    }

    private fun formatWeight(v: Float): String =
        if (v % 1f == 0f) "${v.toInt()} кг" else "%.1f кг".format(v).replace(',', '.')
}
