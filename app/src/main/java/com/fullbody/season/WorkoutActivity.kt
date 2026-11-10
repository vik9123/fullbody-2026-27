package com.fullbody.season

import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fullbody.season.data.ExerciseDef
import com.fullbody.season.data.LogStore
import com.fullbody.season.data.Prefs
import com.fullbody.season.data.Program
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate

class WorkoutActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DAY = "day"
    }

    private data class SetRow(
        val exercise: ExerciseDef,
        val weight: EditText,
        val reps: EditText,
        val done: CheckBox,
    )

    private val rows = mutableListOf<SetRow>()
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        val dayId = intent.getStringExtra(EXTRA_DAY) ?: "TUE"
        val plan = Program.dayById(dayId)
        val cycleWeek = Prefs.cycleWeek(this)

        findViewById<TextView>(R.id.dayTitle).text = plan.title
        val mode = when (cycleWeek) {
            1 -> "Калибровочная неделя: 3 подхода вместо 4"
            8 -> "Разгрузочная неделя: подходов вдвое меньше"
            else -> if (plan.heavy) "Запас 2 повторения в каждом подходе" else "Запас 2–3 повторения, работа до жжения"
        }
        findViewById<TextView>(R.id.daySubtitle).text = "${plan.subtitle} · $mode"

        findViewById<View>(R.id.timerStop).setOnClickListener { stopTimer() }

        val container = findViewById<LinearLayout>(R.id.exerciseContainer)
        container.addView(warmupCard(plan.warmupExtra))
        val last = LogStore.lastSessionBefore(this, dayId, LocalDate.now().toString())
        plan.exercises.forEachIndexed { index, ex ->
            val targetSets = Program.setsForWeek(ex.sets, cycleWeek)
            val prev = last?.exercises?.firstOrNull { it.name == ex.name }
            container.addView(exerciseCard(index + 1, ex, targetSets, plan.heavy, prev))
        }

        findViewById<View>(R.id.finishButton).setOnClickListener { finishWorkout(plan.id) }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun warmupCard(extra: String): View {
        val card = baseCard()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val body = TextView(this).apply {
            text = Program.WARMUP_COMMON + "\n\nСегодня: " + extra
            setTextColor(getColor(R.color.text_dim))
            textSize = 14f
            visibility = View.GONE
        }
        val header = TextView(this).apply {
            text = "Разминка — 8 минут ▾"
            setTextColor(getColor(R.color.accent))
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setOnClickListener {
                body.visibility = if (body.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
        col.addView(header)
        col.addView(body)
        card.addView(col)
        return card
    }

    private fun baseCard(): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(16).toFloat()
        setCardBackgroundColor(getColor(R.color.surface))
        cardElevation = 0f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(14) }
    }

    private fun exerciseCard(
        number: Int,
        ex: ExerciseDef,
        targetSets: Int,
        heavy: Boolean,
        prev: LogStore.ExerciseLog?,
    ): View {
        val card = baseCard()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        col.addView(TextView(this).apply {
            text = "$number. ${ex.name}"
            setTextColor(getColor(R.color.text))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        val repsText = if (ex.repsMin == ex.repsMax) "${ex.repsMax}" else "${ex.repsMin}–${ex.repsMax}"
        val side = if (ex.unilateral) " на сторону" else ""
        col.addView(TextView(this).apply {
            text = "$targetSets × $repsText$side · отдых ${ex.restSec} с · ${ex.muscles}"
            setTextColor(getColor(R.color.text_dim))
            textSize = 13f
            setPadding(0, dp(2), 0, dp(6))
        })

        // Подсказка прогрессии по прошлой тренировке
        if (prev != null && prev.sets.isNotEmpty() && prev.sets.all { it.reps >= ex.repsMax }) {
            col.addView(TextView(this).apply {
                text = if (heavy)
                    "В прошлый раз верх диапазона во всех подходах — добавьте 1,25–2,5 кг"
                else
                    "Верх диапазона набран — поднимите вес и вернитесь к нижней границе повторений"
                setTextColor(getColor(R.color.ok))
                textSize = 13f
                setPadding(0, 0, 0, dp(6))
            })
        }

        // Техника (скрытый блок)
        val techBody = TextView(this).apply {
            text = ex.technique
            setTextColor(getColor(R.color.text_dim))
            textSize = 13f
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(6))
        }
        col.addView(TextView(this).apply {
            text = "Техника ▾"
            setTextColor(getColor(R.color.accent))
            textSize = 13f
            setOnClickListener {
                techBody.visibility = if (techBody.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        })
        col.addView(techBody)

        // Подходы
        for (i in 1..targetSets) {
            val prevSet = prev?.sets?.getOrNull(i - 1)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, 0)
            }
            row.addView(TextView(this).apply {
                text = "$i"
                setTextColor(getColor(R.color.text_dim))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val weight = EditText(this).apply {
                hint = "кг"
                setHintTextColor(getColor(R.color.text_dim))
                setTextColor(getColor(R.color.text))
                textSize = 15f
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                gravity = Gravity.CENTER
                if (prevSet != null && prevSet.weight > 0f) setText(formatWeight(prevSet.weight))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val reps = EditText(this).apply {
                hint = "повт"
                setHintTextColor(getColor(R.color.text_dim))
                setTextColor(getColor(R.color.text))
                textSize = 15f
                inputType = InputType.TYPE_CLASS_NUMBER
                gravity = Gravity.CENTER
                if (prevSet != null && prevSet.reps > 0) setText(prevSet.reps.toString())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val done = CheckBox(this).apply {
                setOnCheckedChangeListener { _, checked ->
                    if (checked) startTimer(ex.restSec, ex.name)
                }
            }
            row.addView(weight)
            row.addView(reps)
            row.addView(done)
            col.addView(row)
            rows.add(SetRow(ex, weight, reps, done))
        }
        card.addView(col)
        return card
    }

    private fun formatWeight(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else v.toString()

    private fun startTimer(seconds: Int, label: String) {
        timer?.cancel()
        val bar = findViewById<View>(R.id.timerBar)
        val text = findViewById<TextView>(R.id.timerText)
        bar.visibility = View.VISIBLE
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val s = millisUntilFinished / 1000 + 1
                text.text = "Отдых: $s с · $label"
            }

            override fun onFinish() {
                text.text = "Отдых окончен — вперёд!"
                try {
                    RingtoneManager.getRingtone(
                        this@WorkoutActivity,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    )?.play()
                    @Suppress("DEPRECATION")
                    (getSystemService(VIBRATOR_SERVICE) as Vibrator)
                        .vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                } catch (_: Exception) {
                }
                bar.postDelayed({ bar.visibility = View.GONE }, 4000)
            }
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
        findViewById<View>(R.id.timerBar).visibility = View.GONE
    }

    private fun finishWorkout(dayId: String) {
        val byExercise = rows
            .filter { it.done.isChecked && it.reps.text.toString().toIntOrNull() != null }
            .groupBy { it.exercise.name }
        if (byExercise.isEmpty()) {
            Toast.makeText(this, "Отметьте хотя бы один подход", Toast.LENGTH_SHORT).show()
            return
        }
        val exercises = byExercise.map { (name, setRows) ->
            LogStore.ExerciseLog(
                name,
                setRows.map {
                    LogStore.SetLog(
                        it.weight.text.toString().replace(',', '.').toFloatOrNull() ?: 0f,
                        it.reps.text.toString().toInt(),
                    )
                },
            )
        }
        LogStore.saveSession(this, LogStore.Session(LocalDate.now().toString(), dayId, exercises))
        Toast.makeText(this, "Тренировка сохранена", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
