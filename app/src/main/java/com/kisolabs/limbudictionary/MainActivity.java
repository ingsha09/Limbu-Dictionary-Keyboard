package com.kisolabs.limbudictionary;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private String selectedAlphabet = "";
    private boolean isShowingBookmarks = false;

    private ArrayList<HashMap<String, Object>> filtered_list = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> word_list = new ArrayList<>();

    private LinearLayout linear_toolbar;
    private LinearLayout linear_search_container;
    private FrameLayout frame_layout6;
    private TextView text_app_title;
    private ImageView favorites;
    private ImageView info_button;
    private ImageView icon_search;
    private EditText edittext_search;
    private ImageView icon_clear;
    private ListView listview1;
    private LinearLayout linear_empty_state;
    private LinearLayout linear_alphabet_index;
    private TextView text_empty_msg;
    private ImageView keyboard_button;

    private Typeface appFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-Edge Setup synchronized with Compose Settings Activity design system
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        int bgColor = 0xFFFCFDFE;
        getWindow().setStatusBarColor(bgColor);
        getWindow().setNavigationBarColor(bgColor);

        WindowInsetsControllerCompat controller = 
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        setContentView(R.layout.main);
        initialize(savedInstanceState);
        initializeLogic();
    }

    private void initialize(Bundle savedInstanceState) {
        linear_toolbar = findViewById(R.id.linear_toolbar);
        linear_search_container = findViewById(R.id.linear_search_container);
        frame_layout6 = findViewById(R.id.frame_layout6);
        text_app_title = findViewById(R.id.text_app_title);
        favorites = findViewById(R.id.favorites);
        keyboard_button = findViewById(R.id.keyboard_button);
        info_button = findViewById(R.id.info_button);
        icon_search = findViewById(R.id.icon_search);
        edittext_search = findViewById(R.id.edittext_search);
        icon_clear = findViewById(R.id.icon_clear);
        listview1 = findViewById(R.id.listview1);
        linear_empty_state = findViewById(R.id.linear_empty_state);
        linear_alphabet_index = findViewById(R.id.linear_alphabet_index);
        text_empty_msg = findViewById(R.id.text_empty_msg);

        View rootLayout = findViewById(R.id.linear_root);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        try {
            appFont = Typeface.createFromAsset(getAssets(), "fonts/googlesans.ttf");
        } catch (Exception e) {
            appFont = Typeface.DEFAULT;
        }

        favorites.setOnClickListener(v -> {
            if (!isShowingBookmarks) {
                listview1.setTag(listview1.getFirstVisiblePosition());
            }

            isShowingBookmarks = !isShowingBookmarks;
            selectedAlphabet = "";

            if (linear_alphabet_index != null) {
                for (int j = 0; j < linear_alphabet_index.getChildCount(); j++) {
                    View child = linear_alphabet_index.getChildAt(j);
                    if (child instanceof TextView) {
                        TextView childTv = (TextView) child;
                        childTv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                        childTv.setTextColor(Color.parseColor("#8C929B"));
                    }
                }
            }

            if (!edittext_search.getText().toString().isEmpty()) {
                edittext_search.setText("");
            }

            if (isShowingBookmarks) {
                var bookmarkPrefs = getSharedPreferences("bookmarked_words", Context.MODE_PRIVATE);
                filtered_list.clear();
                for (HashMap<String, Object> map : word_list) {
                    String limbu = map.get("limbu") != null ? map.get("limbu").toString().trim() : (map.get("limbu_script") != null ? map.get("limbu_script").toString().trim() : "");
                    if (!limbu.isEmpty() && bookmarkPrefs.getBoolean(limbu, false)) {
                        filtered_list.add(map);
                    }
                }

                if (listview1.getAdapter() != null) {
                    ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                }

                listview1.setSelection(0);

                if (filtered_list.isEmpty()) {
                    text_empty_msg.setText("No saved bookmarks yet");
                    if (linear_empty_state != null) linear_empty_state.setVisibility(View.VISIBLE);
                    listview1.setVisibility(View.GONE);
                    if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.GONE);
                } else {
                    if (linear_empty_state != null) linear_empty_state.setVisibility(View.GONE);
                    listview1.setVisibility(View.VISIBLE);
                    if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.VISIBLE);
                }
            } else {
                filtered_list.clear();
                filtered_list.addAll(word_list);

                if (listview1.getAdapter() != null) {
                    ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                }

                if (linear_empty_state != null) linear_empty_state.setVisibility(View.GONE);
                listview1.setVisibility(View.VISIBLE);
                if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.VISIBLE);

                if (listview1.getTag() != null) {
                    listview1.setSelection((int) listview1.getTag());
                }
            }
        });

        if (keyboard_button != null) {
            keyboard_button.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, com.kisolabs.limbudictionary.keyboard.SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        info_button.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();

            ScrollView scrollView = new ScrollView(MainActivity.this);
            LinearLayout mainContainer = new LinearLayout(MainActivity.this);
            mainContainer.setOrientation(LinearLayout.VERTICAL);
            mainContainer.setPadding(48, 48, 48, 36);

            GradientDrawable dialogBg = new GradientDrawable();
            dialogBg.setColor(Color.parseColor("#FFFFFF"));
            dialogBg.setCornerRadius(16f);
            mainContainer.setBackground(dialogBg);

            TextView titleTv = new TextView(MainActivity.this);
            titleTv.setText("Limbu Dictionary & Keyboard");
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            titleTv.setTypeface(appFont, Typeface.BOLD);
            titleTv.setTextColor(Color.parseColor("#1F2328"));
            mainContainer.addView(titleTv);

            int totalWords = (word_list != null) ? word_list.size() : 0;

            String versionName = "1.0";
            try {
                versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                e.printStackTrace();
            }

            TextView subtitleTv = new TextView(MainActivity.this);
            subtitleTv.setText("Version " + versionName + " • " + totalWords + " Words Loaded");
            subtitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            subtitleTv.setTextColor(Color.parseColor("#656D76"));
            subtitleTv.setPadding(0, 4, 0, 16);
            mainContainer.addView(subtitleTv);

            TextView descTv = new TextView(MainActivity.this);
            descTv.setText("A modern dictionary & input keyboard app dedicated to preserving and promoting the Limbu (Yakthung) language and Sirijanga script.\n\nNow includes an integrated native Limbu Keyboard IME with Sirijanga script support, haptic feedback, and dynamic themes.\n\nDeveloped by Ingsha Hang Subba (Kiso Labs).");
            descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            descTv.setTextColor(Color.parseColor("#1F2328"));
            descTv.setPadding(0, 0, 0, 20);
            mainContainer.addView(descTv);

            LinearLayout supportRow = new LinearLayout(MainActivity.this);
            supportRow.setOrientation(LinearLayout.HORIZONTAL);
            supportRow.setGravity(Gravity.CENTER_VERTICAL);
            supportRow.setPadding(24, 20, 24, 20);

            GradientDrawable supportBg = new GradientDrawable();
            supportBg.setColor(Color.parseColor("#FCE8E6"));
            supportBg.setCornerRadius(10f);
            supportBg.setStroke(1, Color.parseColor("#FAD2CF"));
            supportRow.setBackground(supportBg);

            ImageView supportIcon = new ImageView(MainActivity.this);
            supportIcon.setImageResource(R.drawable.ic_support);
            supportIcon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            supportRow.addView(supportIcon);

            TextView supportTv = new TextView(MainActivity.this);
            supportTv.setText("Support / Contribute Project");
            supportTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            supportTv.setTypeface(appFont, Typeface.BOLD);
            supportTv.setTextColor(Color.parseColor("#1F2328"));
            supportTv.setPadding(20, 0, 0, 0);
            supportRow.addView(supportTv);

            supportRow.setOnClickListener(v1 -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://razorpay.me/@ingshahangsubba"));
                startActivity(intent);
            });
            mainContainer.addView(supportRow);

            View space1 = new View(MainActivity.this);
            space1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 12));
            mainContainer.addView(space1);

            LinearLayout githubRow = new LinearLayout(MainActivity.this);
            githubRow.setOrientation(LinearLayout.HORIZONTAL);
            githubRow.setGravity(Gravity.CENTER_VERTICAL);
            githubRow.setPadding(24, 20, 24, 20);

            GradientDrawable itemBg1 = new GradientDrawable();
            itemBg1.setColor(Color.parseColor("#FCFDFE"));
            itemBg1.setCornerRadius(10f);
            itemBg1.setStroke(1, Color.parseColor("#E1E4E8"));
            githubRow.setBackground(itemBg1);

            ImageView githubIcon = new ImageView(MainActivity.this);
            githubIcon.setImageResource(R.drawable.github);
            githubIcon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            githubRow.addView(githubIcon);

            TextView githubTv = new TextView(MainActivity.this);
            githubTv.setText("GitHub API Repository");
            githubTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            githubTv.setTypeface(appFont, Typeface.BOLD);
            githubTv.setTextColor(Color.parseColor("#1F2328"));
            githubTv.setPadding(20, 0, 0, 0);
            githubRow.addView(githubTv);

            githubRow.setOnClickListener(v1 -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ingsha09/limbu-dictionary-api"));
                startActivity(intent);
            });
            mainContainer.addView(githubRow);

            View space2 = new View(MainActivity.this);
            space2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 12));
            mainContainer.addView(space2);

            LinearLayout emailRow = new LinearLayout(MainActivity.this);
            emailRow.setOrientation(LinearLayout.HORIZONTAL);
            emailRow.setGravity(Gravity.CENTER_VERTICAL);
            emailRow.setPadding(24, 20, 24, 20);

            GradientDrawable itemBg2 = new GradientDrawable();
            itemBg2.setColor(Color.parseColor("#FCFDFE"));
            itemBg2.setCornerRadius(10f);
            itemBg2.setStroke(1, Color.parseColor("#E1E4E8"));
            emailRow.setBackground(itemBg2);

            ImageView emailIcon = new ImageView(MainActivity.this);
            emailIcon.setImageResource(R.drawable.communication);
            emailIcon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            emailRow.addView(emailIcon);

            TextView emailTv = new TextView(MainActivity.this);
            emailTv.setText("Send Feedback / Support");
            emailTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            emailTv.setTypeface(appFont, Typeface.BOLD);
            emailTv.setTextColor(Color.parseColor("#1F2328"));
            emailTv.setPadding(20, 0, 0, 0);
            emailRow.addView(emailTv);

            emailRow.setOnClickListener(v1 -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:ingshalimbu09@gmail.com"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Limbu Dictionary & Keyboard Feedback");
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "No email client found", Toast.LENGTH_SHORT).show();
                }
            });
            mainContainer.addView(emailRow);

            TextView closeBtn = new TextView(MainActivity.this);
            closeBtn.setText("CLOSE");
            closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            closeBtn.setTypeface(appFont, Typeface.BOLD);
            closeBtn.setTextColor(Color.parseColor("#1F2328"));
            closeBtn.setGravity(Gravity.END);
            closeBtn.setPadding(0, 24, 12, 0);
            closeBtn.setOnClickListener(v1 -> dialog.dismiss());
            mainContainer.addView(closeBtn);

            scrollView.addView(mainContainer);
            dialog.setView(scrollView);
            dialog.show();
        });

        edittext_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                icon_clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                if (!query.isEmpty() && !selectedAlphabet.isEmpty()) {
                    selectedAlphabet = "";
                    if (linear_alphabet_index != null) {
                        for (int j = 0; j < linear_alphabet_index.getChildCount(); j++) {
                            View child = linear_alphabet_index.getChildAt(j);
                            if (child instanceof TextView) {
                                TextView childTv = (TextView) child;
                                childTv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                                childTv.setTextColor(Color.parseColor("#8C929B"));
                            }
                        }
                    }
                }

                filtered_list.clear();

                if (query.isEmpty()) {
                    if (isShowingBookmarks) {
                        var bookmarkPrefs = getSharedPreferences("bookmarked_words", Context.MODE_PRIVATE);
                        for (HashMap<String, Object> map : word_list) {
                            String limbu = map.get("limbu") != null ? map.get("limbu").toString().trim() : (map.get("limbu_script") != null ? map.get("limbu_script").toString().trim() : "");
                            if (!limbu.isEmpty() && bookmarkPrefs.getBoolean(limbu, false)) {
                                filtered_list.add(map);
                            }
                        }
                    } else {
                        filtered_list.addAll(word_list);
                    }
                } else {
                    ArrayList<HashMap<String, Object>> exactMatches = new ArrayList<>();
                    ArrayList<HashMap<String, Object>> partialMatches = new ArrayList<>();
                    var bookmarkPrefs = isShowingBookmarks ? getSharedPreferences("bookmarked_words", Context.MODE_PRIVATE) : null;

                    for (HashMap<String, Object> map : word_list) {
                        String limbu = map.get("limbu") != null ? map.get("limbu").toString().trim() : (map.get("limbu_script") != null ? map.get("limbu_script").toString().trim() : "");

                        if (isShowingBookmarks && (limbu.isEmpty() || !bookmarkPrefs.getBoolean(limbu, false))) {
                            continue;
                        }

                        String limbuLower = limbu.toLowerCase();
                        String phonetic = map.get("phonetic") != null ? map.get("phonetic").toString().toLowerCase() : (map.get("limbu_roman") != null ? map.get("limbu_roman").toString().toLowerCase() : "");

                        String english = "";
                        String nepali = "";

                        if (map.get("meaning") != null && map.get("meaning") instanceof Map) {
                            Map<String, Object> meaningMap = (Map<String, Object>) map.get("meaning");
                            if (meaningMap.get("en") != null) english = meaningMap.get("en").toString().toLowerCase();
                            if (meaningMap.get("ne") != null) nepali = meaningMap.get("ne").toString().toLowerCase();
                        } else {
                            if (map.get("english") != null) english = map.get("english").toString().toLowerCase();
                            if (map.get("nepali") != null) nepali = map.get("nepali").toString().toLowerCase();
                        }

                        String cleanEnglish = english.replace(".", "").replace(",", "").trim();
                        String cleanNepali = nepali.replace("।", "").replace(",", "").trim();

                        if (limbuLower.equals(query) || phonetic.equals(query) || cleanEnglish.equals(query) || cleanNepali.equals(query)) {
                            exactMatches.add(map);
                        } else if (limbuLower.contains(query) || phonetic.contains(query) || english.contains(query) || nepali.contains(query)) {
                            partialMatches.add(map);
                        }
                    }

                    filtered_list.addAll(exactMatches);
                    filtered_list.addAll(partialMatches);
                }

                if (listview1.getAdapter() == null) {
                    listview1.setAdapter(new Listview1Adapter(filtered_list));
                } else {
                    ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                }

                listview1.setSelection(0);

                if (filtered_list.isEmpty()) {
                    text_empty_msg.setText(isShowingBookmarks ? "No saved bookmarks match search" : "No matching words found");
                    if (linear_empty_state != null) linear_empty_state.setVisibility(View.VISIBLE);
                    listview1.setVisibility(View.GONE);
                    if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.GONE);
                } else {
                    if (linear_empty_state != null) linear_empty_state.setVisibility(View.GONE);
                    listview1.setVisibility(View.VISIBLE);
                    if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.VISIBLE);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        icon_clear.setOnClickListener(v -> {
            edittext_search.setText("");
            if (linear_alphabet_index != null && filtered_list != null && !filtered_list.isEmpty()) {
                linear_alphabet_index.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initializeLogic() {
        text_app_title.setTypeface(appFont, Typeface.BOLD);
        edittext_search.setTypeface(appFont, Typeface.NORMAL);
        text_empty_msg.setTypeface(appFont, Typeface.NORMAL);

        listview1.setFriction(ViewConfiguration.getScrollFriction() * 0.6f);

        isShowingBookmarks = false;
        selectedAlphabet = "";

        listview1.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        listview1.setDividerHeight(0);

        word_list = new ArrayList<>();
        filtered_list = new ArrayList<>();

        // 1. Initial Priority: Load bundled assets/data.json first
        String assetJson = loadJsonFromAssets("data.json");
        if (assetJson != null && !assetJson.trim().isEmpty()) {
            parseAndApplyJson(assetJson);
        }

        // 2. Secondary Priority: Override with cached network response if available from prior runs
        var cachePrefs = getSharedPreferences("dictionary_cache", Context.MODE_PRIVATE);
        String cachedResponse = cachePrefs.getString("json_data", "");
        if (!cachedResponse.isEmpty()) {
            parseAndApplyJson(cachedResponse);
        }

        // Initialize local keyboard dictionary
        com.kisolabs.limbudictionary.keyboard.LimbuDictionaryHelper.INSTANCE.load(getApplicationContext());

        // 3. Silent Background Sync: Update data.json and limbu_words.txt from GitHub
        fetchBackgroundUpdates();

        // Scroll Listener
        listview1.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {}

            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (filtered_list == null || filtered_list.isEmpty() || firstVisibleItem < 0 || firstVisibleItem >= filtered_list.size()) return;

                HashMap<String, Object> topItem = filtered_list.get(firstVisibleItem);
                String limbu = topItem.get("limbu") != null ? topItem.get("limbu").toString() : (topItem.get("limbu_script") != null ? topItem.get("limbu_script").toString() : "");

                if (!limbu.isEmpty()) {
                    String currentLetter = limbu.substring(0, 1);
                    if (!currentLetter.equals(selectedAlphabet)) {
                        selectedAlphabet = currentLetter;

                        if (linear_alphabet_index != null) {
                            for (int j = 0; j < linear_alphabet_index.getChildCount(); j++) {
                                View child = linear_alphabet_index.getChildAt(j);
                                if (child instanceof TextView) {
                                    TextView childTv = (TextView) child;
                                    boolean active = childTv.getText().toString().equals(selectedAlphabet);

                                    childTv.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
                                    childTv.setTextColor(active ? Color.parseColor("#1F2328") : Color.parseColor("#8C929B"));
                                }
                            }
                        }
                    }
                }
            }
        });

        // Alphabet Index Setup
        if (linear_alphabet_index != null) {
    linear_alphabet_index.removeAllViews();
    String[] limbuAlphabet = {"ᤀ", "ᤁ", "ᤂ", "ᤃ", "ᤄ", "ᤅ", "ᤆ", "ᤇ", "ᤈ", "ᤋ", "ᤌ", "ᤍ", "ᤎ", "ᤏ", "ᤐ", "ᤑ", "ᤒ", "ᤓ", "ᤔ", "ᤕ", "ᤖ", "ᤗ", "ᤘ", "ᤙ", "ᤛ", "ᤜ"};

    boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

    // Font size bump for landscape
    float textSizeSp = isLandscape ? 14f : 11f;

    for (final String letter : limbuAlphabet) {
        TextView tv = new TextView(this);
        tv.setText(letter);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        boolean isInitialSelected = letter.equals(selectedAlphabet);
        tv.setTypeface(Typeface.DEFAULT, isInitialSelected ? Typeface.BOLD : Typeface.NORMAL);
        tv.setTextColor(isInitialSelected ? Color.parseColor("#1F2328") : Color.parseColor("#8C929B"));
        tv.setGravity(Gravity.CENTER);

        // Portrait: 0dp + weight 1.0f fills full height from top to bottom
        // Landscape: WRAP_CONTENT + weight 0 allows scrolling without squishing
        LinearLayout.LayoutParams params;
        if (isLandscape) {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
            tv.setPadding(0, paddingPx, 0, paddingPx);
        } else {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f
            );
            tv.setPadding(0, 0, 0, 0);
        }
        
        tv.setLayoutParams(params);

        tv.setOnClickListener(v -> {
            selectedAlphabet = letter;

            for (int j = 0; j < linear_alphabet_index.getChildCount(); j++) {
                View child = linear_alphabet_index.getChildAt(j);
                if (child instanceof TextView) {
                    TextView childTv = (TextView) child;
                    boolean active = childTv.getText().toString().equals(selectedAlphabet);

                    childTv.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
                    childTv.setTextColor(active ? Color.parseColor("#1F2328") : Color.parseColor("#8C929B"));
                }
            }

            int matchIndex = -1;
            for (int i = 0; i < filtered_list.size(); i++) {
                HashMap<String, Object> item = filtered_list.get(i);
                String limbu = item.get("limbu") != null ? item.get("limbu").toString() : (item.get("limbu_script") != null ? item.get("limbu_script").toString() : "");
                if (limbu.startsWith(selectedAlphabet)) {
                    matchIndex = i;
                    break;
                }
            }

            if (matchIndex != -1) {
                listview1.setSelection(matchIndex);
            } else {
                Toast.makeText(getApplicationContext(), "No words starting with " + selectedAlphabet + (isShowingBookmarks ? " in bookmarks" : ""), Toast.LENGTH_SHORT).show();
            }
        });

        linear_alphabet_index.addView(tv);
    }
}

        checkAndShowFirstTimeTutorial();
    }

    private void parseAndApplyJson(String jsonString) {
        try {
            Type type = new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType();
            ArrayList<HashMap<String, Object>> parsedList = new Gson().fromJson(jsonString, type);
            if (parsedList != null && !parsedList.isEmpty()) {
                word_list.clear();
                word_list.addAll(parsedList);
                
                if (!isShowingBookmarks && edittext_search.getText().toString().isEmpty()) {
                    filtered_list.clear();
                    filtered_list.addAll(word_list);

                    if (listview1.getAdapter() == null) {
                        listview1.setAdapter(new Listview1Adapter(filtered_list));
                    } else {
                        ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                    }

                    if (linear_empty_state != null) linear_empty_state.setVisibility(View.GONE);
                    listview1.setVisibility(View.VISIBLE);
                    if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadJsonFromAssets(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fetchBackgroundUpdates() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Fetch data.json
                String dataUrl = "https://raw.githubusercontent.com/ingsha09/limbu-dictionary-api/main/data.json";
                String jsonResponse = downloadUrlToString(dataUrl);

                if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                    var cachePrefs = getSharedPreferences("dictionary_cache", Context.MODE_PRIVATE);
                    cachePrefs.edit().putString("json_data", jsonResponse).apply();

                    runOnUiThread(() -> parseAndApplyJson(jsonResponse));
                }

                // 2. Fetch limbu_words.txt
                String wordsUrl = "https://raw.githubusercontent.com/ingsha09/limbu-dictionary-api/main/limbu_words.txt";
                String wordsTxt = downloadUrlToString(wordsUrl);

                if (wordsTxt != null && !wordsTxt.trim().isEmpty()) {
                    com.kisolabs.limbudictionary.keyboard.LimbuDictionaryHelper.INSTANCE.updateWordsFromRemote(getApplicationContext(), wordsTxt);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String downloadUrlToString(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                in.close();
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- SEQUENTIAL ONBOARDING TUTORIAL LOGIC ---

    private void checkAndShowFirstTimeTutorial() {
        var prefs = getSharedPreferences("app_onboarding", Context.MODE_PRIVATE);
        boolean tutorialShown = prefs.getBoolean("has_seen_toolbar_tutorial", false);

        if (!tutorialShown) {
            linear_toolbar.postDelayed(this::showKeyboardIconSpotlight, 400);
        }
    }

    private void markTutorialAsCompleted() {
        var prefs = getSharedPreferences("app_onboarding", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("has_seen_toolbar_tutorial", true).apply();
    }

    private void showKeyboardIconSpotlight() {
        if (keyboard_button == null || isFinishing()) return;

        int[] location = new int[2];
        keyboard_button.getLocationInWindow(location);

        final FrameLayout rootLayout = (FrameLayout) getWindow().getDecorView().getRootView();

        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#80000000"));
        overlay.setClickable(true);
        overlay.setFocusable(true);

        View targetGlow = new View(this);
        GradientDrawable glowBg = new GradientDrawable();
        glowBg.setShape(GradientDrawable.OVAL);
        glowBg.setColor(Color.parseColor("#262196F3"));
        glowBg.setStroke(4, Color.parseColor("#2196F3"));
        targetGlow.setBackground(glowBg);

        int glowSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(glowSize, glowSize);
        glowParams.leftMargin = location[0] - (glowSize - keyboard_button.getWidth()) / 2;
        glowParams.topMargin = location[1] - (glowSize - keyboard_button.getHeight()) / 2;
        overlay.addView(targetGlow, glowParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(36, 28, 36, 28);
        card.setBackgroundResource(R.drawable.bg_tooltip_card);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        }

        TextView title = new TextView(this);
        title.setText("Enable Limbu Keyboard ⌨️");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(appFont, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1F2328"));

        TextView desc = new TextView(this);
        desc.setText("Tap here to open settings and activate the Limbu Keyboard on your phone.");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        desc.setTypeface(appFont, Typeface.NORMAL);
        desc.setTextColor(Color.parseColor("#656D76"));
        desc.setPadding(0, 6, 0, 14);

        TextView actionBtn = new TextView(this);
        actionBtn.setText("NEXT  →");
        actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        actionBtn.setTypeface(appFont, Typeface.BOLD);
        actionBtn.setTextColor(Color.parseColor("#2196F3"));
        actionBtn.setGravity(Gravity.END);

        card.addView(title);
        card.addView(desc);
        card.addView(actionBtn);

        int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        cardParams.leftMargin = Math.max(24, location[0] - cardWidth + keyboard_button.getWidth() + 20);
        cardParams.topMargin = location[1] + keyboard_button.getHeight() + 16;
        overlay.addView(card, cardParams);

        rootLayout.addView(overlay);

        overlay.setOnClickListener(v -> {
            rootLayout.removeView(overlay);
            showBookmarkIconSpotlight();
        });
    }

    private void showBookmarkIconSpotlight() {
        if (favorites == null || isFinishing()) return;

        int[] location = new int[2];
        favorites.getLocationInWindow(location);

        final FrameLayout rootLayout = (FrameLayout) getWindow().getDecorView().getRootView();

        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#80000000"));
        overlay.setClickable(true);
        overlay.setFocusable(true);

        View targetGlow = new View(this);
        GradientDrawable glowBg = new GradientDrawable();
        glowBg.setShape(GradientDrawable.OVAL);
        glowBg.setColor(Color.parseColor("#26FFC107"));
        glowBg.setStroke(4, Color.parseColor("#FFB300"));
        targetGlow.setBackground(glowBg);

        int glowSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(glowSize, glowSize);
        glowParams.leftMargin = location[0] - (glowSize - favorites.getWidth()) / 2;
        glowParams.topMargin = location[1] - (glowSize - favorites.getHeight()) / 2;
        overlay.addView(targetGlow, glowParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(36, 28, 36, 28);
        card.setBackgroundResource(R.drawable.bg_tooltip_card);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        }

        TextView title = new TextView(this);
        title.setText("Saved Bookmarks ⭐");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(appFont, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1F2328"));

        TextView desc = new TextView(this);
        desc.setText("Tap here to filter and view all your saved dictionary words offline.");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        desc.setTypeface(appFont, Typeface.NORMAL);
        desc.setTextColor(Color.parseColor("#656D76"));
        desc.setPadding(0, 6, 0, 14);

        TextView actionBtn = new TextView(this);
        actionBtn.setText("GOT IT!");
        actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        actionBtn.setTypeface(appFont, Typeface.BOLD);
        actionBtn.setTextColor(Color.parseColor("#FF8F00"));
        actionBtn.setGravity(Gravity.END);

        card.addView(title);
        card.addView(desc);
        card.addView(actionBtn);

        int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        cardParams.leftMargin = Math.max(24, location[0] - cardWidth + favorites.getWidth() + 20);
        cardParams.topMargin = location[1] + favorites.getHeight() + 16;
        overlay.addView(card, cardParams);

        rootLayout.addView(overlay);

        overlay.setOnClickListener(v -> {
            rootLayout.removeView(overlay);
            markTutorialAsCompleted();
        });
    }

    @Override
    public void onBackPressed() {
        if (isShowingBookmarks) {
            isShowingBookmarks = false;
            selectedAlphabet = "";

            filtered_list.clear();
            filtered_list.addAll(word_list);

            if (listview1.getAdapter() != null) {
                ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
            }

            if (linear_empty_state != null) linear_empty_state.setVisibility(View.GONE);
            listview1.setVisibility(View.VISIBLE);

            if (linear_alphabet_index != null) {
                linear_alphabet_index.setVisibility(View.VISIBLE);

                for (int j = 0; j < linear_alphabet_index.getChildCount(); j++) {
                    View child = linear_alphabet_index.getChildAt(j);
                    if (child instanceof TextView) {
                        TextView childTv = (TextView) child;
                        childTv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                        childTv.setTextColor(Color.parseColor("#8C929B"));
                    }
                }
            }

            if (listview1.getTag() != null) {
                listview1.setSelection((int) listview1.getTag());
            }
        } else {
            super.onBackPressed();
        }
    }

    public class Listview1Adapter extends BaseAdapter {

        ArrayList<HashMap<String, Object>> _data;

        public Listview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override public int getCount() { return _data.size(); }
        @Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
        @Override public long getItemId(int _index) { return _index; }

        @Override
        public View getView(final int _position, View _v, ViewGroup _container) {
            LayoutInflater _inflater = getLayoutInflater();
            View _view = _v;
            if (_view == null) {
                _view = _inflater.inflate(R.layout.word_item, null);
            }

            final LinearLayout card_background = _view.findViewById(R.id.card_background);
            final View item_divider = _view.findViewById(R.id.item_divider);
            final TextView text_meaning_nepali = _view.findViewById(R.id.text_meaning_nepali);
            final TextView text_meaning_english = _view.findViewById(R.id.text_meaning_english);
            final TextView text_limbu = _view.findViewById(R.id.text_limbu);
            final TextView text_roman = _view.findViewById(R.id.text_roman);
            final ImageView btn_share = _view.findViewById(R.id.btn_share);
            final ImageView btn_bookmark = _view.findViewById(R.id.btn_bookmark);

            if (_position < 0 || _position >= filtered_list.size()) return _view;

            final HashMap<String, Object> map = filtered_list.get(_position);

            if (text_limbu.getTag(R.id.info_button) == null) {
                text_limbu.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                text_roman.setTypeface(appFont);
                text_meaning_nepali.setTypeface(appFont);
                text_meaning_english.setTypeface(appFont);

                text_limbu.setTextIsSelectable(true);
                text_roman.setTextIsSelectable(true);
                text_meaning_nepali.setTextIsSelectable(true);
                text_meaning_english.setTextIsSelectable(true);

                text_limbu.setTag(R.id.info_button, true);
            }

            String limbuStr = map.get("limbu") != null ? String.valueOf(map.get("limbu")) : String.valueOf(map.get("limbu_script"));
            text_limbu.setText(limbuStr);

            String romanStr = map.get("phonetic") != null ? String.valueOf(map.get("phonetic")) : String.valueOf(map.get("limbu_roman"));
            if (!romanStr.trim().isEmpty() && !romanStr.equals("—")) {
                text_roman.setText(romanStr);
                text_roman.setVisibility(View.VISIBLE);
            } else {
                text_roman.setVisibility(View.GONE);
            }

            String nepaliStr = "";
            String englishStr = "";

            if (map.get("meaning") != null && map.get("meaning") instanceof Map) {
                Map<String, Object> meaningMap = (Map<String, Object>) map.get("meaning");
                if (meaningMap.get("ne") != null) nepaliStr = String.valueOf(meaningMap.get("ne"));
                if (meaningMap.get("en") != null) englishStr = String.valueOf(meaningMap.get("en"));
            } else {
                if (map.get("nepali") != null) nepaliStr = String.valueOf(map.get("nepali"));
                if (map.get("english") != null) englishStr = String.valueOf(map.get("english"));
            }

            if (!nepaliStr.trim().isEmpty() && !nepaliStr.equals("—")) {
                text_meaning_nepali.setText(nepaliStr);
                text_meaning_nepali.setVisibility(View.VISIBLE);
            } else {
                text_meaning_nepali.setVisibility(View.GONE);
            }

            if (!englishStr.trim().isEmpty() && !englishStr.equals("—")) {
                text_meaning_english.setText(englishStr);
                text_meaning_english.setVisibility(View.VISIBLE);
            } else {
                text_meaning_english.setVisibility(View.GONE);
            }

            text_limbu.setTextColor(Color.parseColor("#1F2328"));
            text_meaning_nepali.setTextColor(Color.parseColor("#1F2328"));
            text_meaning_english.setTextColor(Color.parseColor("#656D76"));

            // Reset view properties directly
            _view.animate().cancel();
            _view.setTranslationY(0f);
            _view.setAlpha(1f);

            var bookmarkPrefs = _view.getContext().getSharedPreferences("bookmarked_words", Context.MODE_PRIVATE);
            final String wordKey = limbuStr.trim();
            boolean isBookmarked = bookmarkPrefs.getBoolean(wordKey, false);

            if (btn_bookmark != null) {
                btn_bookmark.setImageResource(isBookmarked ? R.drawable.ic_star_fill : R.drawable.ic_star_outline);
                btn_bookmark.setOnClickListener(v -> {
                    boolean currentlySaved = bookmarkPrefs.getBoolean(wordKey, false);
                    boolean newState = !currentlySaved;

                    bookmarkPrefs.edit().putBoolean(wordKey, newState).apply();

                    if (v instanceof ImageView) {
                        ((ImageView) v).setImageResource(newState ? R.drawable.ic_star_fill : R.drawable.ic_star_outline);
                    }

                    if (isShowingBookmarks && !newState) {
                        filtered_list.remove(map);
                        if (listview1.getAdapter() != null) {
                            ((BaseAdapter) listview1.getAdapter()).notifyDataSetChanged();
                        }

                        if (filtered_list.isEmpty()) {
                            text_empty_msg.setText("No saved bookmarks yet");
                            if (linear_empty_state != null) linear_empty_state.setVisibility(View.VISIBLE);
                            listview1.setVisibility(View.GONE);
                            if (linear_alphabet_index != null) linear_alphabet_index.setVisibility(View.GONE);
                        }
                    }
                });
            }

            if (btn_share != null) {
                final String shareLimbu = limbuStr;
                final String shareRoman = romanStr;
                final String shareNepali = nepaliStr;
                final String shareEnglish = englishStr;

                btn_share.setOnClickListener(v -> {
                    StringBuilder shareBody = new StringBuilder();
                    shareBody.append(shareLimbu);
                    if (!shareRoman.isEmpty() && !shareRoman.equals("—")) {
                        shareBody.append(" (").append(shareRoman).append(")");
                    }
                    shareBody.append("\n");

                    if (!shareNepali.isEmpty() && !shareNepali.equals("—")) {
                        shareBody.append("🇳🇵 ").append(shareNepali).append("\n");
                    }
                    if (!shareEnglish.isEmpty() && !shareEnglish.equals("—")) {
                        shareBody.append("🇬🇧 ").append(shareEnglish).append("\n");
                    }
                    shareBody.append("\nVia Limbu Dictionary & Keyboard App");

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody.toString().trim());
                    startActivity(Intent.createChooser(shareIntent, "Share Word"));
                });
            }

            return _view;
        }
    }
}
