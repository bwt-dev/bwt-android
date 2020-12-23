package dev.bwt.app

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import java.text.SimpleDateFormat
import java.util.*


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val bitcoindUrl: EditTextPreference? = findPreference("bitcoind_url")
            bitcoindUrl?.setOnBindEditTextListener { editText -> editText.isSingleLine = true }
            val bitcoindAuth: EditTextPreference? = findPreference("bitcoind_auth")
            bitcoindAuth?.setOnBindEditTextListener { editText -> editText.isSingleLine = true }
            val bitcoindWallet: EditTextPreference? = findPreference("bitcoind_wallet")
            bitcoindWallet?.setOnBindEditTextListener { editText -> editText.isSingleLine = true }
            val bitcoindDir: EditTextPreference? = findPreference("bitcoind_dir")
            bitcoindDir?.setOnBindEditTextListener { editText -> editText.isSingleLine = true }
            val electrumAddr: EditTextPreference? = findPreference("electrum_addr")
            electrumAddr?.setOnBindEditTextListener { editText -> editText.isSingleLine = true }

            bitcoindWallet?.summaryProvider =
                Preference.SummaryProvider<EditTextPreference> { preference ->
                    val text = preference.text
                    if (TextUtils.isEmpty(text)) {
                        "For use with multi-wallet. Leave blank to use the default wallet."
                    } else {
                        text
                    }
                }
            bitcoindDir?.summaryProvider =
                Preference.SummaryProvider<EditTextPreference> { preference ->
                    val text = preference.text
                    if (TextUtils.isEmpty(text)) {
                        "For the cookie file (local bitcoind only). Leave blank to use the username/password."
                    } else {
                        text
                    }
                }
            /*
            val gapLimit: EditTextPreference? = findPreference("gap_limit")
            gapLimit?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            val initialImportSize: EditTextPreference? = findPreference("initial_import_size")
            initialImportSize?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            val pollInterval: EditTextPreference? = findPreference("poll_interval")
            pollInterval?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }*/
        }

        override fun onDisplayPreferenceDialog(preference: Preference?) {
            if (preference is DatePickerPreference) {
                val datepickerdialog = DatePickerPreferenceDialog.newInstance(preference.key)
                datepickerdialog.setTargetFragment(this, 0)
                datepickerdialog.show(parentFragmentManager, "DatePickerDialog")
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }
}

class DatePickerPreference(context: Context?, attrs: AttributeSet?) : DialogPreference(
    context,
    attrs
) {

    fun getPersistedDate(): Date {
        // defaults to 2013-01-10, the date BIP 30 was proposed
        val unixTs = super.getPersistedInt(1378771200)
        Log.d("bwt", "loaded unixts $unixTs, date ${Date(unixTs.toLong() * 1000)}")
        return Date(unixTs.toLong() * 1000)
    }

    fun persistDate(date: Date) {
        Log.d("bwt", "saving $date, ts ${(date.time / 1000).toInt()}")
        super.persistInt((date.time / 1000).toInt())
        notifyChanged()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        summary = formatter.format(getPersistedDate())
    }
}


class DatePickerPreferenceDialog : PreferenceDialogFragmentCompat() {

    lateinit var datepicker: DatePicker

    override fun onCreateDialogView(context: Context?): View {
        val ctx = ContextThemeWrapper(context, R.style.CustomDatePickerDialogTheme)
        datepicker = DatePicker(ctx)
        datepicker.calendarViewShown = false;
        return datepicker
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        var cal = Calendar.getInstance()
        cal.time = (preference as DatePickerPreference).getPersistedDate()
        Log.d("bwt", "onBindDialogView $cal")

        datepicker.updateDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            var cal = Calendar.getInstance()
            cal.set(datepicker.year, datepicker.month, datepicker.dayOfMonth)
            Log.d("bwt", "onDialogClosed persist $cal")
            (preference as DatePickerPreference).persistDate(cal.time)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            preference.summary = formatter.format(cal.time)
        }
    }

    companion object {
        fun newInstance(key: String): DatePickerPreferenceDialog {
            val fragment = DatePickerPreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}