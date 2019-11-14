package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.ortiz.touchview.TouchImageView;

import java.io.File;
import fr.tvbarthel.apps.cameracolorpicker.R;


public class GraphActivity extends AppCompatActivity {
    protected TouchImageView graph;
    protected TextView name;
    String graphFileName = "graph.jpeg";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_graph_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        graph = findViewById(R.id.graph);
        name = findViewById(R.id.folder_name);
        String folderName = getIntent().getStringExtra("folderName");
        File imageFile = new File(getExternalFilesDir(folderName),graphFileName);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), imageFile.getAbsolutePath());
        graph.setImageDrawable(bitmapDrawable);
        name.setText(folderName);
        setTitle(R.string.graph);
    }
}
