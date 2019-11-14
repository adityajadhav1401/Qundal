package fr.tvbarthel.apps.cameracolorpicker.activities;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.tvbarthel.apps.cameracolorpicker.R;

public class History extends AppCompatActivity {
    protected ListView listView;
    protected Context context;
    SharedPreferences sharedpreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedpreferences = getSharedPreferences("DefaultPref", Context.MODE_PRIVATE);
        Locale locale = new Locale(sharedpreferences.getString("language", "en"));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        setContentView(R.layout.activity_history);
        listView = findViewById(R.id.list_view);
        context = this;
        final ArrayList<item> listItems = new ArrayList<>();
        File defaultFolder = new File(getExternalFilesDir(null).toString());
        final File[] files = defaultFolder.listFiles();
        for (File file : files) {
            if (file.isDirectory() && !file.getName().equals("matching_images")) {
                Date date = new Date(file.lastModified());
                listItems.add(new item(file.getName().toString(),
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date)));
            }
        }

        ListAdapter adapter = new ListAdapter(this, listItems);
        listView.setAdapter(adapter);
        setTitle(R.string.previous);
    }

    class item {
        String title, subtitle;

        public item(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public class ListAdapter extends BaseAdapter {


        ArrayList<item> listItems;
        Context context;

        public ListAdapter(Context context, ArrayList<item> listItems) {
            this.context = context;
            this.listItems = listItems;

        }

        @Override
        public int getCount() {
            return listItems.size();
        }

        @Override
        public Object getItem(int arg0) {
            return listItems.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(R.layout.history_list_item, arg2, false);

            AppCompatButton delete= row.findViewById(R.id.delete_button);
            delete.setTag(arg0);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int pos = (int)view.getTag();
                    final File toDelete = new File(getExternalFilesDir(listItems.get(pos).title).toString());
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(getResources().getString(R.string.delete));
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogLayout = inflater.inflate(R.layout.delete_alert,null);
                    builder.setPositiveButton(getResources().getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (toDelete.exists()) deleteRecursive(toDelete);
                                    listItems.remove(pos);
                                    ListAdapter.this.notifyDataSetChanged();
                                }
                            });
                    builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setView(dialogLayout);
                    builder.setCancelable(false);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextView name = view.findViewById(R.id.text1);
                    Intent intent = new Intent(History.this,OptionsActivity.class);
                    Bundle b = new Bundle();
                    b.putString("folderName",name.getText().toString());
                    b.putBoolean("isSave",false);
                    intent.putExtras(b);
                    startActivity(intent);
                }
            });
            TextView title = row.findViewById(R.id.text1);
            title.setText(listItems.get(arg0).title);

            TextView subtitle = row.findViewById(R.id.text2);
            subtitle.setText(listItems.get(arg0).subtitle);
            return row;
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}
