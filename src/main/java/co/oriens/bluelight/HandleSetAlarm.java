package co.oriens.bluelight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.util.Collection;

import co.github.androidutils.logger.Logger;
import co.oriens.bluelight.model.AlarmsManager;
import co.oriens.bluelight.model.interfaces.Alarm;
import co.oriens.bluelight.model.interfaces.IAlarmsManager;
import co.oriens.bluelight.model.interfaces.Intents;
import co.oriens.bluelight.presenter.AlarmDetailsActivity;
import co.oriens.bluelight.presenter.AlarmsListActivity;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.EXTRA_HOUR;

public class HandleSetAlarm extends Activity {

    private IAlarmsManager alarms;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        alarms = AlarmsManager.getAlarmsManager();
        Intent intent = getIntent();
        Intent startDetailsIntent = new Intent(this, AlarmDetailsActivity.class);
        if (intent == null || !ACTION_SET_ALARM.equals(intent.getAction())) {
            finish();
            return;
        } else if (!intent.hasExtra(EXTRA_HOUR)) {
            // no extras - start list activity
            startActivity(new Intent(this, AlarmsListActivity.class));
            finish();
            return;
        }

        Alarm alarm = createNewAlarmFromIntent(intent);

        boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        if (!skipUi) {
            startDetailsIntent.putExtra(Intents.EXTRA_ID, alarm.getId());
            startActivity(startDetailsIntent);
        }
        finish();
    }

    /**
     * A new alarm has to be created or an existing one edited based on the
     * intent extras.
     */
    private Alarm createNewAlarmFromIntent(Intent intent) {
        final int hours = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0);
        final int minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
        final String msg = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        final String label = msg == null ? "" : msg;

        Collection<Alarm> sameAlarms = Collections2.filter(alarms.getAlarmsList(), new Predicate<Alarm>() {
            @Override
            public boolean apply(Alarm candidate) {
                boolean hoursMatch = candidate.getHour() == hours;
                boolean minutesMatch = candidate.getMinutes() == minutes;
                boolean labelsMatch = candidate.getLabel() != null && candidate.getLabel().equals(label);
                boolean noRepeating = !candidate.getDaysOfWeek().isRepeatSet();
                return hoursMatch && minutesMatch && labelsMatch && noRepeating;
            }
        });

        Alarm alarm;
        if (sameAlarms.isEmpty()) {
            Logger.getDefaultLogger().d("No alarm found, creating a new one");
            alarm = AlarmsManager.getAlarmsManager().createNewAlarm();
            //@formatter:off
            alarm.edit()
                .setHour(hours)
                .setMinutes(minutes)
                .setLabel(label)
                .setEnabled(true)
                .commit();
        //@formatter:on
        } else {
            Logger.getDefaultLogger().d("Enable existing alarm");
            alarm = sameAlarms.iterator().next();
            alarm.enable(true);
        }
        return alarm;
    }
}