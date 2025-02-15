package com.github.anrimian.musicplayer.ui.player_screen.view.wrappers;

import static android.view.View.INVISIBLE;
import static com.github.anrimian.musicplayer.ui.utils.AndroidUtils.getResourceIdFromAttr;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.databinding.FragmentDrawerBinding;
import com.github.anrimian.musicplayer.databinding.PartialDetailedMusicBinding;
import com.github.anrimian.musicplayer.databinding.PartialQueueToolbarBinding;
import com.github.anrimian.musicplayer.domain.utils.functions.Callback;
import com.github.anrimian.musicplayer.ui.common.toolbar.AdvancedToolbar;
import com.github.anrimian.musicplayer.ui.player_screen.view.slide.ToolbarDelegate;
import com.github.anrimian.musicplayer.ui.player_screen.view.slide.ToolbarVisibilityDelegate;
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.JugglerView;
import com.github.anrimian.musicplayer.ui.utils.views.bottom_sheet.SimpleBottomSheetCallback;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.BoundValuesDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.DelegateManager;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.ExpandViewDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.LeftBottomShadowDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.MotionLayoutDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.MoveXDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.MoveYDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.ReverseDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.SlideDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.TextSizeDelegate;
import com.github.anrimian.musicplayer.ui.utils.views.delegate.VisibilityDelegate;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class PlayerPanelWrapperImpl implements PlayerPanelWrapper {

    private final PartialDetailedMusicBinding panelBinding;
    private final FragmentDrawerBinding viewBinding;
    private final PartialQueueToolbarBinding queueToolbarBinding;
    private final MotionLayout mlBottomSheet;
    @Nullable
    private final CoordinatorLayout bottomSheetCoordinator;
    @Nullable
    private final View bottomSheetLeftShadow;
    @Nullable
    private final View bottomSheetTopLeftShadow;
    private final TextView tvCurrentComposition;
    private final CoordinatorLayout clPlayQueueContainer;
    private final AdvancedToolbar toolbar;
    private final JugglerView contentViewContainer;

    private final Activity activity;
    private final Runnable onBottomSheetDragCollapsed;
    private final Runnable onBottomSheetDragExpanded;
    private final Callback<Boolean> bottomSheetStateListener;

    private final BottomSheetBehavior<View> bottomSheetBehavior;
    private final SlideDelegate bottomSheetDelegate;

    @Nullable
    private Runnable collapseDelayedAction;

    public PlayerPanelWrapperImpl(View view,
                                  FragmentDrawerBinding viewBinding,
                                  PartialDetailedMusicBinding panelBinding,
                                  MotionLayout mlBottomSheet,
                                  Activity activity,
                                  Bundle savedInstanceState,
                                  Runnable onBottomSheetDragCollapsed,
                                  Runnable onBottomSheetDragExpanded,
                                  Callback<Boolean> bottomSheetStateListener) {
        this.viewBinding = viewBinding;
        this.panelBinding = panelBinding;
        this.mlBottomSheet = mlBottomSheet;
        this.activity = activity;
        this.onBottomSheetDragCollapsed = onBottomSheetDragCollapsed;
        this.onBottomSheetDragExpanded = onBottomSheetDragExpanded;
        this.bottomSheetStateListener = bottomSheetStateListener;

        queueToolbarBinding = viewBinding.toolbarPlayQueue;
        clPlayQueueContainer = viewBinding.clPlayQueueContainer;
        bottomSheetCoordinator = view.findViewById(R.id.coordinator_bottom_sheet);
        tvCurrentComposition = panelBinding.tvCurrentComposition;
        toolbar = viewBinding.toolbar;
        bottomSheetLeftShadow = view.findViewById(R.id.bottom_sheet_left_shadow);
        bottomSheetTopLeftShadow = view.findViewById(R.id.bottom_sheet_top_left_shadow);
        contentViewContainer = view.findViewById(R.id.drawer_fragment_container);

        setViewStartState();
        if (savedInstanceState == null) {
            mlBottomSheet.setVisibility(INVISIBLE);
        }

        bottomSheetDelegate = createBottomSheetDelegate();
        bottomSheetBehavior = BottomSheetBehavior.from(mlBottomSheet);
        mlBottomSheet.setClickable(true);
        bottomSheetBehavior.addBottomSheetCallback(new SimpleBottomSheetCallback(
                this::onBottomSheetStateChanged,
                bottomSheetDelegate::onSlide

        ));
    }

    @Override
    public boolean isBottomPanelExpanded() {
        return bottomSheetBehavior.getState() == STATE_EXPANDED;
    }

    @Override
    public void collapseBottomPanel() {
        bottomSheetStateListener.call(false);

        setButtonsSelectableBackground(
                getResourceIdFromAttr(activity, R.attr.selectableItemBackgroundBorderless)
        );

        bottomSheetDelegate.onSlide(0f);
        if (bottomSheetBehavior.getState() != STATE_COLLAPSED) {
            bottomSheetBehavior.setState(STATE_COLLAPSED);
        }
    }

    @Override
    public void collapseBottomPanelSmoothly() {
        bottomSheetStateListener.call(false);

        if (bottomSheetBehavior.getState() != STATE_COLLAPSED) {
            bottomSheetBehavior.setState(STATE_COLLAPSED);
        } else {
            if (collapseDelayedAction != null) {
                collapseDelayedAction.run();
                collapseDelayedAction = null;
            }
        }
    }

    @Override
    public void collapseBottomPanelSmoothly(Runnable doOnCollapse) {
        collapseDelayedAction = doOnCollapse;
        collapseBottomPanelSmoothly();
    }

    @Override
    public void expandBottomPanel() {
        bottomSheetStateListener.call(true);

        setButtonsSelectableBackground(R.drawable.bg_selectable_round_shape);
        toolbar.setControlButtonProgress(1f);

        bottomSheetDelegate.onSlide(1f);
        if (bottomSheetBehavior.getState() != STATE_EXPANDED) {
            bottomSheetBehavior.setState(STATE_EXPANDED);
        }
    }

    @Override
    public void openPlayerPanel() {
        setButtonsSelectableBackground(R.drawable.bg_selectable_round_shape);
        if (bottomSheetBehavior.getState() == STATE_COLLAPSED) {
            bottomSheetBehavior.setState(STATE_EXPANDED);
            bottomSheetDelegate.onSlide(1f);
        }
    }

    private void setButtonsSelectableBackground(@DrawableRes int resId) {
        panelBinding.ivPlayPause.setBackgroundResource(resId);
        panelBinding.ivSkipToNext.setBackgroundResource(resId);
        panelBinding.ivSkipToPrevious.setBackgroundResource(resId);
    }

    private void onBottomSheetStateChanged(Integer newState) {
        switch (newState) {
            case STATE_COLLAPSED: {
                if (collapseDelayedAction != null) {
                    collapseDelayedAction.run();
                    collapseDelayedAction = null;
                }
                onBottomSheetDragCollapsed.run();
                return;
            }
            case STATE_EXPANDED: {
                onBottomSheetDragExpanded.run();
            }
        }
    }

    private SlideDelegate createBottomSheetDelegate() {
        DelegateManager boundDelegateManager = new DelegateManager();
        boundDelegateManager
                .addDelegate(new BoundValuesDelegate(0.4f, 1f, new VisibilityDelegate(queueToolbarBinding.getRoot())))
                .addDelegate(new ReverseDelegate(new BoundValuesDelegate(0.0f, 0.8f, new ToolbarVisibilityDelegate(toolbar))))
                .addDelegate(new BoundValuesDelegate(0f, 0.6f, new ReverseDelegate(new VisibilityDelegate(toolbar.getToolbarModesViewGroup()))))
                .addDelegate(new BoundValuesDelegate(1f, 1f, new ReverseDelegate(new VisibilityDelegate(contentViewContainer))))
                .addDelegate(new TextSizeDelegate(tvCurrentComposition, R.dimen.current_composition_expand_text_size, R.dimen.current_composition_expand_text_size))//no sense
                .addDelegate(new MotionLayoutDelegate(mlBottomSheet))
                .addDelegate(new BoundValuesDelegate(0.7f, 0.95f, new ReverseDelegate(new VisibilityDelegate(viewBinding.drawerFragmentContainer))))
                .addDelegate(new BoundValuesDelegate(0.3f, 1.0f, new ExpandViewDelegate(R.dimen.panel_cover_size, panelBinding.ivMusicIcon)))
                .addDelegate(new BoundValuesDelegate(0.98f, 1.0f, new VisibilityDelegate(panelBinding.pvFileState)))
                .addDelegate(new BoundValuesDelegate(0.95f, 1.0f, new VisibilityDelegate(panelBinding.tvCurrentCompositionAuthor)))
                .addDelegate(new BoundValuesDelegate(0.4f, 1.0f, new VisibilityDelegate(panelBinding.btnActionsMenu)))
                .addDelegate(new BoundValuesDelegate(0.93f, 1.0f, new VisibilityDelegate(panelBinding.sbTrackState)))
                .addDelegate(new BoundValuesDelegate(0.95f, 1.0f, new VisibilityDelegate(panelBinding.tvError)))
                .addDelegate(new BoundValuesDelegate(0.98f, 1.0f, new VisibilityDelegate(panelBinding.btnInfinitePlay)))
                .addDelegate(new BoundValuesDelegate(0.98f, 1.0f, new VisibilityDelegate(panelBinding.btnRandomPlay)))
                .addDelegate(new BoundValuesDelegate(0.97f, 1.0f, new VisibilityDelegate(panelBinding.tvPlayedTime)))
                .addDelegate(new ToolbarDelegate(toolbar, activity.getWindow()))
                .addDelegate(new BoundValuesDelegate(0.97f, 1.0f, new VisibilityDelegate(panelBinding.tvTotalTime)))
                .addDelegate(new BoundValuesDelegate(0.97f, 1.0f, new VisibilityDelegate(panelBinding.tvPlaybackSpeed)))
                .addDelegate(new BoundValuesDelegate(0.97f, 1.0f, new VisibilityDelegate(panelBinding.tvVolume)))
                .addDelegate(new BoundValuesDelegate(0.97f, 1.0f, new VisibilityDelegate(panelBinding.tvSleepTime)))
                .addDelegate(new ReverseDelegate(new BoundValuesDelegate(0.8f, 1f, new VisibilityDelegate(panelBinding.ivBottomPanelIndicator))));

        DelegateManager delegateManager = new DelegateManager();
        if (isInLandscapeOrientation()) {//landscape
            boundDelegateManager.addDelegate(new MoveXDelegate(
                    0.5f,
                    bottomSheetCoordinator));

            boundDelegateManager.addDelegate(new LeftBottomShadowDelegate(
                    bottomSheetLeftShadow,
                    bottomSheetTopLeftShadow,
                    mlBottomSheet,
                    bottomSheetCoordinator));
            delegateManager.addDelegate(new MoveYDelegate(clPlayQueueContainer,
                    0.85f,
                    activity.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_height)
            ));
            boundDelegateManager.addDelegate(new BoundValuesDelegate(0f, 0.1f, new VisibilityDelegate(clPlayQueueContainer)));
        } else {
            boundDelegateManager.addDelegate(new BoundValuesDelegate(0.90f, 1f, new VisibilityDelegate(clPlayQueueContainer)));
            delegateManager.addDelegate(new MoveYDelegate(clPlayQueueContainer, 0.3f));
        }
        delegateManager.addDelegate(new BoundValuesDelegate(0.008f, 0.95f, boundDelegateManager));
        return delegateManager;
    }

    private void setViewStartState() {
        clPlayQueueContainer.setVisibility(INVISIBLE);
        queueToolbarBinding.getRoot().setVisibility(INVISIBLE);
        queueToolbarBinding.getRoot().setAlpha(0f);
        toolbar.getToolbarModesViewGroup().setVisibility(INVISIBLE);
        toolbar.setContentAlpha(0f);
        contentViewContainer.setVisibility(INVISIBLE);
    }

    private boolean isInLandscapeOrientation() {
        return bottomSheetCoordinator != null;
    }
}
