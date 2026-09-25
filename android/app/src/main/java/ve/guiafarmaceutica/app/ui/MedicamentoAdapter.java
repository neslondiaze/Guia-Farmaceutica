package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Objects;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.FichaClinica;
import ve.guiafarmaceutica.app.util.DosificacionHelper;

public class MedicamentoAdapter extends ListAdapter<FichaClinica, MedicamentoAdapter.ViewHolder> {

    public interface OnMedicamentoClickListener {
        void onClick(FichaClinica medicamento);
        void onMonografiaClick(FichaClinica medicamento);
        void onCalcularClick(FichaClinica medicamento);
    }

    private final OnMedicamentoClickListener listener;

    public MedicamentoAdapter(OnMedicamentoClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<FichaClinica> DIFF_CALLBACK = new DiffUtil.ItemCallback<FichaClinica>() {
        @Override
        public boolean areItemsTheSame(@NonNull FichaClinica oldItem, @NonNull FichaClinica newItem) {
            return Objects.equals(oldItem.id, newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull FichaClinica oldItem, @NonNull FichaClinica newItem) {
            return Objects.equals(oldItem.nombre, newItem.nombre) &&
                    Objects.equals(oldItem.laboratorio, newItem.laboratorio) &&
                    Objects.equals(oldItem.atc_codigo, newItem.atc_codigo);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicamento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FichaClinica med = getItem(position);
        holder.bind(med, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombre, laboratorio, atc, via, forma;
        Button btnMonografia, btnCalcular;

        ViewHolder(View view) {
            super(view);
            nombre = view.findViewById(R.id.text_nombre);
            laboratorio = view.findViewById(R.id.text_laboratorio);
            atc = view.findViewById(R.id.text_atc);
            via = view.findViewById(R.id.text_via);
            forma = view.findViewById(R.id.text_forma);
            btnMonografia = view.findViewById(R.id.btn_monografia);
            btnCalcular = view.findViewById(R.id.btn_calcular);
        }

        void bind(FichaClinica med, OnMedicamentoClickListener listener) {
            nombre.setText(med.nombre);
            laboratorio.setText(med.laboratorio);
            atc.setText(String.format("ATC: %s - %s", med.atc_codigo, med.atc_descripcion));
            via.setText(med.via);
            forma.setText(med.forma);
            
            boolean esDosificable = DosificacionHelper.esDosificable(med.via, med.forma, med.nombre);
            btnCalcular.setVisibility(esDosificable ? View.VISIBLE : View.GONE);

            btnMonografia.setOnClickListener(v -> listener.onMonografiaClick(med));
            btnCalcular.setOnClickListener(v -> listener.onCalcularClick(med));

            itemView.setOnClickListener(v -> listener.onClick(med));
        }
    }
}
