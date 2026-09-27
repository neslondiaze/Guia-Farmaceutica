package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import ve.guiafarmaceutica.app.R;

public class SplashActivity extends AppCompatActivity {

    private CountDownTimer timer;
    private boolean transicionNavegada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Button btnSaltar = findViewById(R.id.btn_saltar_splash);
        btnSaltar.setOnClickListener(v -> navegarAMain());

        timer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int segundos = (int) (millisUntilFinished / 1000) + 1;
                if (btnSaltar != null) {
                    btnSaltar.setText("Saltar " + segundos + "s");
                }
            }

            @Override
            public void onFinish() {
                navegarAMain();
            }
        }.start();
    }

    private synchronized void navegarAMain() {
        if (transicionNavegada) return;
        transicionNavegada = true;
        if (timer != null) {
            timer.cancel();
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
}
