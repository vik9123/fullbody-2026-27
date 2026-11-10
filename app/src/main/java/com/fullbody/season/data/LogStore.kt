package com.fullbody.season.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Локальный журнал тренировок: JSON-файл во внутреннем хранилище. */
object LogStore {

    data class SetLog(val weight: Float, val reps: Int)
    data class ExerciseLog(val name: String, val sets: List<SetLog>)
    data class Session(val date: String, val dayId: String, val exercises: List<ExerciseLog>)

    private const val FILE_NAME = "workout_log.json"

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun load(context: Context): MutableList<Session> {
        val f = file(context)
        if (!f.exists()) return mutableListOf()
        return try {
            val root = JSONObject(f.readText())
            val arr = root.getJSONArray("sessions")
            val out = mutableListOf<Session>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                val exArr = s.getJSONArray("exercises")
                val exercises = mutableListOf<ExerciseLog>()
                for (j in 0 until exArr.length()) {
                    val e = exArr.getJSONObject(j)
                    val setArr = e.getJSONArray("sets")
                    val sets = mutableListOf<SetLog>()
                    for (k in 0 until setArr.length()) {
                        val st = setArr.getJSONObject(k)
                        sets.add(SetLog(st.optDouble("w", 0.0).toFloat(), st.optInt("r", 0)))
                    }
                    exercises.add(ExerciseLog(e.getString("name"), sets))
                }
                out.add(Session(s.getString("date"), s.getString("day"), exercises))
            }
            out
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun persist(context: Context, sessions: List<Session>) {
        val root = JSONObject()
        val arr = JSONArray()
        for (s in sessions) {
            val so = JSONObject()
            so.put("date", s.date)
            so.put("day", s.dayId)
            val exArr = JSONArray()
            for (e in s.exercises) {
                val eo = JSONObject()
                eo.put("name", e.name)
                val setArr = JSONArray()
                for (st in e.sets) {
                    val sto = JSONObject()
                    sto.put("w", st.weight.toDouble())
                    sto.put("r", st.reps)
                    setArr.put(sto)
                }
                eo.put("sets", setArr)
                exArr.put(eo)
            }
            so.put("exercises", exArr)
            arr.put(so)
        }
        root.put("sessions", arr)
        file(context).writeText(root.toString())
    }

    /** Сохраняет сессию; запись за ту же дату и тот же день заменяется. */
    fun saveSession(context: Context, session: Session) {
        val sessions = load(context)
        sessions.removeAll { it.date == session.date && it.dayId == session.dayId }
        sessions.add(session)
        sessions.sortBy { it.date }
        persist(context, sessions)
    }

    /** Последняя записанная сессия для дня недели (TUE/THU/SAT) строго раньше даты onDate. */
    fun lastSessionBefore(context: Context, dayId: String, onDate: String): Session? =
        load(context)
            .filter { it.dayId == dayId && it.date < onDate }
            .maxByOrNull { it.date }

    /** История максимального рабочего веса по упражнению: (дата, вес), по возрастанию даты. */
    fun weightHistory(context: Context, exerciseName: String): List<Pair<String, Float>> =
        load(context)
            .sortedBy { it.date }
            .mapNotNull { s ->
                s.exercises.firstOrNull { it.name == exerciseName }
                    ?.sets?.filter { it.weight > 0f }
                    ?.maxOfOrNull { it.weight }
                    ?.let { s.date to it }
            }

    /** Все упражнения, по которым есть записи. */
    fun loggedExerciseNames(context: Context): List<String> =
        load(context).flatMap { s -> s.exercises.map { it.name } }.distinct().sorted()
}
