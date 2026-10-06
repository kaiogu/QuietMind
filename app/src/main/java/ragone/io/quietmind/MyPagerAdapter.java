package ragone.io.quietmind;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

/** The intro, the ten stages of meditation and the four milestones, in reading order. */
public class MyPagerAdapter extends PagerAdapter {

    private static final int NO_STAGE = 0;

    private static final class Page {
        final int layout;
        /** Stage number 1-10, or NO_STAGE for pages without a "DONE" checkbox. */
        final int stage;
        /** Container the checkbox is appended to. */
        final int container;

        Page(int layout, int stage, int container) {
            this.layout = layout;
            this.stage = stage;
            this.container = container;
        }

        static Page plain(int layout) {
            return new Page(layout, NO_STAGE, View.NO_ID);
        }
    }

    private static final Page[] PAGES = {
            Page.plain(R.layout.intro),
            new Page(R.layout.stage1, 1, R.id.layout1),
            new Page(R.layout.stage2, 2, R.id.layout2),
            new Page(R.layout.stage3, 3, R.id.layout3),
            Page.plain(R.layout.milestone1),
            new Page(R.layout.stage4, 4, R.id.layout4),
            new Page(R.layout.stage5, 5, R.id.layout5),
            new Page(R.layout.stage6, 6, R.id.layout6),
            Page.plain(R.layout.milestone2),
            new Page(R.layout.stage7, 7, R.id.layout7),
            Page.plain(R.layout.milestone3),
            new Page(R.layout.stage8, 8, R.id.layout8),
            new Page(R.layout.stage9, 9, R.id.layout9),
            new Page(R.layout.stage10, 10, R.id.layout10),
            Page.plain(R.layout.milestone4),
    };

    private final SparseArray<View> pages = new SparseArray<>();
    private final Context context;
    private final Prefs prefs;

    public MyPagerAdapter(Context context) {
        this.context = context;
        this.prefs = new Prefs(context);
    }

    @Override
    public int getCount() {
        return PAGES.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
        pages.remove(position);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Page page = PAGES[position];
        View view = LayoutInflater.from(context).inflate(page.layout, container, false);

        if (page.stage != NO_STAGE) {
            final int stage = page.stage;
            SmoothCheckBox checkBox = new SmoothCheckBox(context);
            checkBox.setText("DONE");
            int size = CompatUtils.dp2px(context, 60);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, CompatUtils.dp2px(context, 20), 0, 0);
            checkBox.setLayoutParams(params);
            checkBox.setChecked(prefs.isStageDone(stage), false);
            checkBox.setOnCheckedChangeListener((box, isChecked) -> prefs.setStageDone(stage, isChecked));
            ((LinearLayout) view.findViewById(page.container)).addView(checkBox);
        }

        container.addView(view);
        pages.put(position, view);
        return view;
    }
}
