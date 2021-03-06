//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.monke.basemvplib.AppActivityManager;
import com.monke.monkeybook.R;
import com.monke.monkeybook.help.ReadBookControl;
import com.monke.monkeybook.base.MBaseActivity;
import com.monke.monkeybook.dao.DbHelper;
import com.monke.monkeybook.presenter.impl.IReadBookPresenter;
import com.monke.monkeybook.presenter.ReadBookPresenterImpl;
import com.monke.monkeybook.service.ReadAloudService;
import com.monke.monkeybook.utils.DensityUtil;
import com.monke.monkeybook.utils.PremissionCheck;
import com.monke.monkeybook.view.impl.IReadBookView;
import com.monke.monkeybook.view.popupwindow.CheckAddShelfPop;
import com.monke.monkeybook.view.popupwindow.MoreSettingPop;
import com.monke.monkeybook.view.popupwindow.ReadBookMenuMorePop;
import com.monke.monkeybook.view.popupwindow.ReadInterfacePop;
import com.monke.monkeybook.view.popupwindow.WindowLightPop;
import com.monke.monkeybook.widget.ChapterListView;
import com.monke.monkeybook.widget.contentswitchview.BookContentView;
import com.monke.monkeybook.widget.contentswitchview.ContentSwitchView;
import com.monke.monkeybook.widget.modialog.MoProgressHUD;
import com.monke.mprogressbar.MHorProgressBar;
import com.monke.mprogressbar.OnProgressListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.grantland.widget.AutofitTextView;

import static com.monke.monkeybook.presenter.ReadBookPresenterImpl.OPEN_FROM_OTHER;
import static com.monke.monkeybook.service.ReadAloudService.newReadAloudAction;

public class ReadBookActivity extends MBaseActivity<IReadBookPresenter> implements IReadBookView {
    @BindView(R.id.fl_content)
    FrameLayout flContent;
    @BindView(R.id.csv_book)
    ContentSwitchView csvBook;
    //主菜单
    @BindView(R.id.fl_menu)
    FrameLayout flMenu;
    @BindView(R.id.v_menu_bg)
    View vMenuBg;
    @BindView(R.id.ll_menu_top)
    LinearLayout llMenuTop;
    @BindView(R.id.ll_menu_bottom)
    LinearLayout llMenuBottom;
    @BindView(R.id.iv_return)
    ImageButton ivReturn;
    @BindView(R.id.iv_refresh)
    ImageButton ivRefresh;
    @BindView(R.id.iv_more)
    ImageView ivMenuMore;
    @BindView(R.id.atv_title)
    AutofitTextView atvTitle;
    @BindView(R.id.atv_url)
    AutofitTextView atvUrl;
    @BindView(R.id.tv_pre)
    TextView tvPre;
    @BindView(R.id.tv_next)
    TextView tvNext;
    @BindView(R.id.hpb_read_progress)
    MHorProgressBar hpbReadProgress;
    @BindView(R.id.ll_catalog)
    LinearLayout llCatalog;
    @BindView(R.id.ll_light)
    LinearLayout llLight;
    @BindView(R.id.ll_font)
    LinearLayout llFont;
    @BindView(R.id.ll_setting)
    LinearLayout llSetting;
    @BindView(R.id.clp_chapterlist)
    ChapterListView chapterListView;
    @BindView(R.id.ib_read_aloud)
    ImageView ibReadAloud;
    //主菜单动画
    private Animation menuTopIn;
    private Animation menuTopOut;
    private Animation menuBottomIn;
    private Animation menuBottomOut;

    private boolean aloudButton;
    private boolean hideStatusBar;
    private String noteUrl;

    private CheckAddShelfPop checkAddShelfPop;
    private WindowLightPop windowLightPop;
    private ReadBookMenuMorePop readBookMenuMorePop;
    private ReadInterfacePop readInterfacePop;
    private MoreSettingPop moreSettingPop;
    private MoProgressHUD moProgressHUD;
    private ReadBookControl readBookControl;
    private Intent readAloudIntent;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            ReadAloudService readAloudService = ((ReadAloudService.MyBinder) service).getService();
            readAloudService.setAloudServiceListener(new ReadAloudService.AloudServiceListener() {
                @Override
                public void stopService() {
                    unbindService(conn);
                    csvBook.readAloudStop();
                }

                @Override
                public void readAloudNext() {
                    runOnUiThread(() -> csvBook.readAloudNext());
                }

                @Override
                public void showMassage(String msg) {
                    runOnUiThread(() -> toast(msg));
                }
            });
        }
    };

    @Override
    protected IReadBookPresenter initInjector() {
        return new ReadBookPresenterImpl();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            noteUrl = savedInstanceState.getString("noteUrl");
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCreateActivity() {
        setOrientation();
        setContentView(R.layout.activity_book_read);
        hideStatusBar = preferences.getBoolean("hide_status_bar", false);
        readAloudIntent = new Intent(this, ReadAloudService.class);
        readAloudIntent.setAction(newReadAloudAction);
        readBookControl = ReadBookControl.getInstance();
        if (readBookControl.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        hideStatusBar(hideStatusBar);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPresenter.getBookShelf() != null) {
            outState.putString("noteUrl", mPresenter.getBookShelf().getNoteUrl());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideNavigationBar();
        }
    }

    /**
     * 隐藏状态栏
     */
    private void hideStatusBar(Boolean hide) {
        if (hide) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 隐藏虚拟按键
     */
    private void hideNavigationBar() {
        if (preferences.getBoolean("hide_navigation_bar", false)) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    /**
     * 隐藏菜单
     */
    private void popMenuOut() {
        if (flMenu.getVisibility() == View.VISIBLE) {
            llMenuTop.startAnimation(menuTopOut);
            llMenuBottom.startAnimation(menuBottomOut);
        }
    }

    /**
     * 正文事件
     */
    ContentSwitchView.LoadDataListener loadDataListener = new ContentSwitchView.LoadDataListener() {
        @Override
        public void loadData(BookContentView bookContentView, long qtag, int chapterIndex, int pageIndex) {
            mPresenter.loadContent(bookContentView, qtag, chapterIndex, pageIndex);
        }

        @Override
        public void updateProgress(int chapterIndex, int pageIndex) {
            mPresenter.updateProgress(chapterIndex, pageIndex);

            if (mPresenter.getBookShelf().getBookInfoBean().getChapterList().size() > 0) {
                atvTitle.setText(mPresenter.getBookShelf().getBookInfoBean().getChapterList(mPresenter.getBookShelf().getDurChapter()).getDurChapterName());
                atvUrl.setText(mPresenter.getBookShelf().getBookInfoBean().getChapterList(mPresenter.getBookShelf().getDurChapter()).getDurChapterUrl());
            } else {
                atvTitle.setText("无章节");
            }
            if (hpbReadProgress.getDurProgress() != chapterIndex + 1)
                hpbReadProgress.setDurProgress(chapterIndex + 1);
        }

        @Override
        public String getChapterTitle(int chapterIndex) {
            return mPresenter.getChapterTitle(chapterIndex);
        }

        @Override
        public void initData(int lineCount) {
            mPresenter.setPageLineCount(lineCount);
            mPresenter.setPageWidth(csvBook.getContentWidth());
            mPresenter.initContent();
        }

        @Override
        public void showMenu() {
            flMenu.setVisibility(View.VISIBLE);
            llMenuTop.startAnimation(menuTopIn);
            llMenuBottom.startAnimation(menuBottomIn);
            hideStatusBar(false);
            hideNavigationBar();
        }

        @Override
        public void readAloud(String content) {
            readAloudIntent.putExtra("aloudButton", aloudButton);
            readAloudIntent.putExtra("content", content);
            startService(readAloudIntent);
            aloudButton = false;
        }
    };

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void initData() {
        mPresenter.saveProgress();
        //显示菜单
        menuTopIn = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_top_in);
        menuTopIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                hideStatusBar(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                vMenuBg.setOnClickListener(v -> popMenuOut());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        menuBottomIn = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_bottom_in);
        //隐藏菜单
        menuTopOut = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_top_out);
        menuTopOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                vMenuBg.setOnClickListener(null);
                hideStatusBar(hideStatusBar);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                flMenu.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        menuBottomOut = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_bottom_out);
    }

    @Override
    protected void bindView() {
        ButterKnife.bind(this);
        initCsvBook();
        //弹窗
        moProgressHUD = new MoProgressHUD(this);
        //界面设置
        readInterfacePop = new ReadInterfacePop(this, new ReadInterfacePop.OnChangeProListener() {
            @Override
            public void textChange(int index) {
                csvBook.changeTextSize();
            }

            @Override
            public void bgChange(int index) {
                csvBook.changeBg();
            }
        });
        //目录
        chapterListView.setOnChangeListener(new ChapterListView.OnChangeListener() {
            @Override
            public void animIn() {
                hideStatusBar(false);
            }

            @Override
            public void animOut() {
                hideStatusBar(hideStatusBar);
            }
        });
        //其它设置
        moreSettingPop = new MoreSettingPop(this, keepScreenOn -> {
            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        //亮度设置
        windowLightPop = new WindowLightPop(this);
        windowLightPop.initLight();
        readBookMenuMorePop = new ReadBookMenuMorePop(this);
    }

    @Override
    public void setHpbReadProgressMax(int count) {
        hpbReadProgress.setMaxProgress(count);
    }

    private void initCsvBook() {
        csvBook.bookReadInit(() -> mPresenter.initData(ReadBookActivity.this));
    }

    @Override
    public void initPop() {
        checkAddShelfPop = new CheckAddShelfPop(this, mPresenter.getBookShelf().getBookInfoBean().getName(), new CheckAddShelfPop.OnItemClickListener() {
            @Override
            public void clickExit() {
                finish();
            }

            @Override
            public void clickAddShelf() {
                mPresenter.addToShelf(null);
                checkAddShelfPop.dismiss();
            }
        });
        chapterListView.setData(mPresenter.getBookShelf(), index -> csvBook
                .setInitData(index, mPresenter.getBookShelf().getBookInfoBean().getChapterList().size(),
                        BookContentView.DurPageIndexBegin));
    }

    @Override
    protected void bindEvent() {
        //阅读进度
        hpbReadProgress.setProgressListener(new OnProgressListener() {
            @Override
            public void moveStartProgress(float dur) {

            }

            @Override
            public void durProgressChange(float dur) {

            }

            @Override
            public void moveStopProgress(float dur) {
                int realDur = (int) Math.ceil(dur);
                if (realDur < 1) {
                    realDur = 1;
                }
                if ((realDur - 1) != mPresenter.getBookShelf().getDurChapter()) {
                    csvBook.setInitData(realDur - 1,
                            mPresenter.getBookShelf().getBookInfoBean().getChapterList().size(),
                            BookContentView.DurPageIndexBegin);
                }
                if (hpbReadProgress.getDurProgress() != realDur)
                    hpbReadProgress.setDurProgress(realDur);
            }

            @Override
            public void setDurProgress(float dur) {
                if (hpbReadProgress.getMaxProgress() == 1) {
                    tvPre.setEnabled(false);
                    tvNext.setEnabled(false);
                } else {
                    if (dur == 1) {
                        tvPre.setEnabled(false);
                        tvNext.setEnabled(true);
                    } else if (dur == hpbReadProgress.getMaxProgress()) {
                        tvPre.setEnabled(true);
                        tvNext.setEnabled(false);
                    } else {
                        tvPre.setEnabled(true);
                        tvNext.setEnabled(true);
                    }
                }
            }
        });

        //菜单
        ivMenuMore.setOnClickListener(view -> readBookMenuMorePop
                .showAsDropDown(ivMenuMore, 0, DensityUtil.dp2px(ReadBookActivity.this, -3.5f)));

        //正文
        csvBook.setLoadDataListener(loadDataListener);

        //返回按钮
        ivReturn.setOnClickListener(view -> finish());

        //离线下载
        readBookMenuMorePop.setOnClickDownload(view -> {
            readBookMenuMorePop.dismiss();
            ReadBookActivity.this.popMenuOut();
            if (mPresenter.getBookShelf() != null) {
                //弹出离线下载界面
                int endIndex = mPresenter.getBookShelf().getDurChapter() + 50;
                if (endIndex >= mPresenter.getBookShelf().getBookInfoBean().getChapterList().size()) {
                    endIndex = mPresenter.getBookShelf().getBookInfoBean().getChapterList().size() - 1;
                }
                moProgressHUD.showDownloadList(mPresenter.getBookShelf().getDurChapter(), endIndex,
                        mPresenter.getBookShelf().getBookInfoBean().getChapterList().size(),
                        (start, end) -> {
                            moProgressHUD.dismiss();
                            mPresenter.addDownload(start, end);
                        });
            }
        });

        //换源
        readBookMenuMorePop.setOnClickChangeSource(view -> {
            readBookMenuMorePop.dismiss();
            ReadBookActivity.this.popMenuOut();
            if (mPresenter.getBookShelf() != null) {
                moProgressHUD.showChangeSource(mPresenter.getBookShelf(), searchBookBean -> {
                    mPresenter.changeBookSource(searchBookBean);
                    csvBook.showLoading();
                });
            }
        });

        //刷新按钮
        ivRefresh.setOnClickListener(view -> {
            ReadBookActivity.this.popMenuOut();
            if (mPresenter.getBookShelf() != null) {
                DbHelper.getInstance().getmDaoSession().getBookContentBeanDao().deleteByKey(mPresenter.getBookShelf()
                        .getDurChapterListBean().getDurChapterUrl());
                mPresenter.getBookShelf().getDurChapterListBean()
                        .setBookContentBean(null);
                csvBook.setInitData(mPresenter.getBookShelf().getDurChapter(),
                        mPresenter.getBookShelf().getBookInfoBean().getChapterList().size(),
                        BookContentView.DurPageIndexBegin);
            }
        });

        //打开URL
        atvUrl.setOnClickListener(view -> {
            try {
                String url = atvUrl.getText().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.can_not_open, Toast.LENGTH_SHORT).show();
            }
        });

        //朗读
        ibReadAloud.setOnClickListener(view -> {
            ReadBookActivity.this.popMenuOut();
            if (mPresenter.getBookShelf() != null) {
                aloudButton = true;
                csvBook.readAloudStart();
                ReadBookActivity.this.bindService(readAloudIntent, conn, Context.BIND_AUTO_CREATE);
            }
        });

        //上一章
        tvPre.setOnClickListener(view -> {
            if (mPresenter.getBookShelf() != null) {
                csvBook.setInitData(mPresenter.getBookShelf().getDurChapter() - 1,
                        mPresenter.getBookShelf().getBookInfoBean().getChapterList().size(),
                        BookContentView.DurPageIndexBegin);
            }
        });

        //下一章
        tvNext.setOnClickListener(view -> {
            if (mPresenter.getBookShelf() != null) {
                csvBook.setInitData(mPresenter.getBookShelf().getDurChapter() + 1,
                        mPresenter.getBookShelf().getBookInfoBean().getChapterList().size(),
                        BookContentView.DurPageIndexBegin);
            }
        });

        //目录
        llCatalog.setOnClickListener(view -> {
            ReadBookActivity.this.popMenuOut();
            if (chapterListView.hasData()) {
                new Handler().postDelayed(() -> chapterListView.show(mPresenter.getBookShelf().getDurChapter()), menuTopOut.getDuration());
            }
        });

        //亮度
        llLight.setOnClickListener(view -> {
            ReadBookActivity.this.popMenuOut();
            new Handler().postDelayed(() -> windowLightPop.showAtLocation(flContent, Gravity.BOTTOM, 0, 0), menuTopOut.getDuration());
        });

        //界面
        llFont.setOnClickListener(view -> {
            ReadBookActivity.this.popMenuOut();
            new Handler().postDelayed(() -> readInterfacePop.showAtLocation(flContent, Gravity.BOTTOM, 0, 0), menuTopOut.getDuration());
        });

        //设置
        llSetting.setOnClickListener(view -> {
            ReadBookActivity.this.popMenuOut();
            new Handler().postDelayed(() -> moreSettingPop.showAtLocation(flContent, Gravity.BOTTOM, 0, 0), menuTopOut.getDuration());
        });

    }

    @Override
    public Paint getPaint() {
        return csvBook.getTextPaint();
    }

    @Override
    public void initContentSuccess(int durChapterIndex, int chapterAll, int durPageIndex) {
        csvBook.setInitData(durChapterIndex, chapterAll, durPageIndex);
    }

    @Override
    public void startLoadingBook() {
        csvBook.startLoading();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPresenter.saveProgress();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Boolean mo = moProgressHUD.onKeyDown(keyCode, event);
        if (mo) {
            return true;
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (flMenu.getVisibility() == View.VISIBLE) {
                    llMenuTop.startAnimation(menuTopOut);
                    llMenuBottom.startAnimation(menuBottomOut);
                    return true;
                } else if (!mPresenter.getAdd() && checkAddShelfPop != null && !checkAddShelfPop.isShowing()) {
                    checkAddShelfPop.showAtLocation(flContent, Gravity.CENTER, 0, 0);
                    return true;
                } else {
                    Boolean temp2 = chapterListView.dismissChapterList();
                    if (temp2)
                        return true;
                    else {
                        finish();
                        return true;
                    }
                }
            } else {
                Boolean temp = csvBook.onKeyDown(keyCode, event);
                if (temp)
                    return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Boolean temp = csvBook.onKeyUp(keyCode, event);
        return temp || super.onKeyUp(keyCode, event);
    }

    @Override
    public void loadLocationBookError() {
        csvBook.loadError();
    }

    @Override
    public void showOnLineView() {
        ivMenuMore.setVisibility(View.VISIBLE);
        ivRefresh.setVisibility(View.VISIBLE);
        atvUrl.setVisibility(View.VISIBLE);
    }

    @Override
    public String getNoteUrl() {
        return noteUrl;
    }

    @Override
    public ContentSwitchView getCsvBook() {
        return csvBook;
    }

    @Override
    public void showLoading(String msg) {
        moProgressHUD.showLoading(msg);
    }

    @Override
    public void dismissLoading() {
        moProgressHUD.dismiss();
    }

    private Boolean showCheckPermission = false;

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0x11) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && PremissionCheck.checkPremission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                mPresenter.openBookFromOther(ReadBookActivity.this);
            } else {
                if (!this.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showCheckPermission = true;
                    moProgressHUD.showTwoButton("去系统设置打开SD卡读写权限？", "取消", v -> finish(), "设置", v -> PremissionCheck.requestPermissionSetting(ReadBookActivity.this));
                } else {
                    Toast.makeText(this, "未获取SD卡读取权限", Toast.LENGTH_SHORT).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showCheckPermission && mPresenter.getOpen_from() == OPEN_FROM_OTHER && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !PremissionCheck.checkPremission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            showCheckPermission = true;
            mPresenter.openBookFromOther(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(readAloudIntent);
    }

    @Override
    public void finish() {
        if (!AppActivityManager.getInstance().isExist(MainActivity.class)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        super.finish();
    }

}