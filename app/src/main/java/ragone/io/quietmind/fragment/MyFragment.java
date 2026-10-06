package ragone.io.quietmind.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import me.relex.circleindicator.CircleIndicator;
import ragone.io.quietmind.MyPagerAdapter;
import ragone.io.quietmind.Prefs;
import ragone.io.quietmind.R;

/** Swipeable stages of meditation, reopening on the page the user last looked at. */
public class MyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.viewpager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Prefs prefs = new Prefs(view.getContext());
        ViewPager viewpager = view.findViewById(R.id.viewpager);
        CircleIndicator indicator = view.findViewById(R.id.indicator);
        viewpager.setAdapter(new MyPagerAdapter(view.getContext()));
        indicator.setViewPager(viewpager);
        viewpager.setCurrentItem(prefs.getLastViewedStage());
        viewpager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                prefs.setLastViewedStage(position);
            }
        });
    }
}
