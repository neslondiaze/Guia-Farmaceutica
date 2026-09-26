package ve.guiafarmaceutica.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.AppDatabase;
import ve.guiafarmaceutica.app.viewmodel.CategoryViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar BD en segundo plano (siembra la semilla si es necesario)
        new Thread(() -> AppDatabase.obtener(this)).start();

        setupGrid();

        findViewById(R.id.btn_search).setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
        });
    }

    private void setupGrid() {
        RecyclerView recyclerView = findViewById(R.id.categories_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Clases Anatómicas", R.drawable.ic_cat_anatomy, CategoryViewModel.TYPE_ANATOMICA));
        categories.add(new Category("Grupos Terapéuticos", R.drawable.ic_cat_therapy, CategoryViewModel.TYPE_GRUPO));
        categories.add(new Category("Principios Activos", R.drawable.ic_cat_science, CategoryViewModel.TYPE_PRINCIPIO));
        categories.add(new Category("Vías de Administración", R.drawable.ic_cat_route, CategoryViewModel.TYPE_VIA));
        categories.add(new Category("Formas Farmacéuticas", R.drawable.ic_cat_form, CategoryViewModel.TYPE_FORMA));
        categories.add(new Category("Laboratorios", R.drawable.ic_cat_factory, CategoryViewModel.TYPE_LABORATORIO));
        categories.add(new Category("Seguridad (Embarazo)", R.drawable.ic_cat_security, CategoryViewModel.TYPE_SEGURIDAD));
        categories.add(new Category("Monografías Clínicas", R.drawable.ic_cat_monography, CategoryViewModel.TYPE_MONOGRAFIA));
        categories.add(new Category("Índice Alfabético", R.drawable.ic_cat_alphabet, CategoryViewModel.TYPE_ALFABETICO));
        categories.add(new Category("Impresión Diagnóstica", R.drawable.ic_cat_monography, "cat_impresion_diagnostica"));
        categories.add(new Category("Gestión de Pacientes", R.drawable.ic_cat_security, "cat_gestion_pacientes"));
        categories.add(new Category("Configuración", R.drawable.ic_cat_settings, "cat_settings"));
        categories.add(new Category("Centro de Ayuda", R.drawable.ic_cat_help, CategoryViewModel.TYPE_AYUDA));

        recyclerView.setAdapter(new CategoryAdapter(categories));
    }

    static class Category {
        String name;
        int iconRes;
        String type;

        Category(String name, int iconRes, String type) {
            this.name = name;
            this.iconRes = iconRes;
            this.type = type;
        }
    }

    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<Category> categories;

        CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category cat = categories.get(position);
            holder.name.setText(cat.name);
            holder.icon.setImageResource(cat.iconRes);
            holder.itemView.setOnClickListener(v -> {
                if ("cat_impresion_diagnostica".equalsIgnoreCase(cat.type)) {
                    v.getContext().startActivity(new Intent(v.getContext(), CrearImpresionActivity.class));
                } else if ("cat_gestion_pacientes".equalsIgnoreCase(cat.type)) {
                    v.getContext().startActivity(new Intent(v.getContext(), GestionClinicaActivity.class));
                } else if ("cat_settings".equalsIgnoreCase(cat.type)) {
                    v.getContext().startActivity(new Intent(v.getContext(), SettingsActivity.class));
                } else {
                    Intent intent = new Intent(v.getContext(), CategoryActivity.class);
                    intent.putExtra(CategoryActivity.EXTRA_TYPE, cat.type);
                    intent.putExtra(CategoryActivity.EXTRA_TITLE, cat.name);
                    v.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;

            ViewHolder(View view) {
                super(view);
                icon = view.findViewById(R.id.category_icon);
                name = view.findViewById(R.id.category_name);
            }
        }
    }
}
