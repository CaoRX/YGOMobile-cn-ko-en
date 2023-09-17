package cn.garymb.ygomobile.ex_card;

import static cn.garymb.ygomobile.Constants.ASSET_SERVER_LIST;
import static cn.garymb.ygomobile.Constants.URL_YGO233_ADVANCE;
import static cn.garymb.ygomobile.Constants.URL_YGO233_FILE;
import static cn.garymb.ygomobile.Constants.URL_YGO233_FILE_ALT;
import static cn.garymb.ygomobile.utils.DownloadUtil.TYPE_DOWNLOAD_EXCEPTION;
import static cn.garymb.ygomobile.utils.ServerUtil.AddServer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.bean.ServerInfo;
import cn.garymb.ygomobile.bean.ServerList;
import cn.garymb.ygomobile.bean.events.ExCardEvent;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.activities.WebActivity;
import cn.garymb.ygomobile.ui.home.MainActivity;
import cn.garymb.ygomobile.ui.home.ServerListManager;
import cn.garymb.ygomobile.ui.plus.VUiKit;
import cn.garymb.ygomobile.utils.DownloadUtil;
import cn.garymb.ygomobile.utils.FileUtils;
import cn.garymb.ygomobile.utils.IOUtils;
import cn.garymb.ygomobile.utils.LogUtil;
import cn.garymb.ygomobile.utils.ServerUtil;
import cn.garymb.ygomobile.utils.SharedPreferenceUtil;
import cn.garymb.ygomobile.utils.SystemUtils;
import cn.garymb.ygomobile.utils.UnzipUtils;
import cn.garymb.ygomobile.utils.XmlUtils;
import cn.garymb.ygomobile.utils.YGOUtil;
import ocgcore.DataManager;


public class ExCardListFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = String.valueOf(ExCardListFragment.class);
    private Context context;
    private View layoutView;
    private ExCardListAdapter mExCardListAdapter;
    private RecyclerView mExCardListView;
    private LinearLayout ll_Download;
    private TextView textDownload;
    private int FailedCount;

    /**
     * ���ڱ�־��ǰ����״̬�����ڷ�ֹ�û�����ظ���������ذ�ť��
     * Mark the download state, which can prevent user from clicking the download button
     * repeatedly.
     */
    enum DownloadState {
        DOWNLOAD_ING,
        NO_DOWNLOAD
    }

    private DownloadState downloadState;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        layoutView = inflater.inflate(R.layout.fragment_ex_card_list, container, false);

        this.context = getContext();

        initView(layoutView);
        if (!EventBus.getDefault().isRegistered(this)) {//�����ж�
            EventBus.getDefault().register(this);
        }
        return layoutView;
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.i(TAG, "excard fragment on stop");
        if (EventBus.getDefault().isRegistered(this))//�����ж�
            EventBus.getDefault().unregister(this);
    }

    public void initView(View layoutView) {
        mExCardListView = layoutView.findViewById(R.id.list_ex_card);
        mExCardListAdapter = new ExCardListAdapter(R.layout.item_ex_card);
        //RecyclerView: No layout manager attached; skipping layout
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        mExCardListView.setLayoutManager(linearLayoutManager);
        mExCardListView.setAdapter(mExCardListAdapter);
        mExCardListAdapter.loadData();
        textDownload = layoutView.findViewById(R.id.text_download_prerelease);
        ll_Download = layoutView.findViewById(R.id.btn_download_prerelease);
        ll_Download.setOnClickListener(this);
        changeDownloadText();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_download_prerelease:
                LogUtil.i(TAG, "start download");
                if (downloadState != DownloadState.DOWNLOAD_ING) {
                    downloadState = DownloadState.DOWNLOAD_ING;
                    downloadfromWeb(URL_YGO233_FILE);
                }
                break;
        }

    }

    /**
     * �������п���״̬�ı䰴ť��ʽ
     */
    public void changeDownloadText() {
        if (ServerUtil.exCardState == ServerUtil.ExCardState.UPDATED) {
            //btn_downloadչʾĬ����ͼ
            textDownload.setText(R.string.tip_redownload);
        } else if (ServerUtil.exCardState == ServerUtil.ExCardState.NEED_UPDATE) {
            textDownload.setText(R.string.Download);
        } else if (ServerUtil.exCardState == ServerUtil.ExCardState.ERROR) {
            Toast.makeText(getActivity(), R.string.ex_card_check_toast_message_iii, Toast.LENGTH_LONG).show();
            WebActivity.open(getActivity(), getString(R.string.ex_card_list_title), URL_YGO233_ADVANCE);
        }
    }
    //TODO eventbus receive event

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DownloadUtil.TYPE_DOWNLOAD_ING:
                    textDownload.setText(msg.arg1 + "%");
                    break;
                case DownloadUtil.TYPE_DOWNLOAD_EXCEPTION:
                    ++FailedCount;
                    if (FailedCount <= 2) {
                        Toast.makeText(getActivity(), R.string.Ask_to_Change_Other_Way, Toast.LENGTH_SHORT).show();
                        downloadfromWeb(URL_YGO233_FILE_ALT);
                    }
                    YGOUtil.showTextToast("error" + msg.obj);
                    break;
                case UnzipUtils.ZIP_READY:
                    textDownload.setText(R.string.title_use_ex);
                    break;
                case UnzipUtils.ZIP_UNZIP_OK:

                    /* �����з�������Ϣ��ӵ��������б��� */
                    String servername = "";
                    if (AppsSettings.get().getDataLanguage() == 0)
                        servername = "23333���з�����";
                    if (AppsSettings.get().getDataLanguage() == 1)
                        servername = "YGOPRO ?? ?? ????";
                    if (AppsSettings.get().getDataLanguage() == 2)
                        servername = "Mercury23333 OCG/TCG Pre-release";
                    AddServer(getActivity(), servername, "s1.ygo233.com", 23333, "Knight of Hanoi");
                    //changeDownloadButton();��������ɺ�ͨ��EventBus֪ͨ������ɣ������û�������غ���ʱ�г���fragment������������ɺ��лأ�ͨ��eventbus�ܱ�֤��ť��ʽ��ȷ����

                    /* ע�⣬Ҫ�ȸ��°汾�� */
                    SharedPreferenceUtil.setExpansionDataVer(ServerUtil.serverExCardVersion);
                    ServerUtil.exCardState = ServerUtil.ExCardState.UPDATED;
                    EventBus.getDefault().postSticky(new ExCardEvent(ExCardEvent.EventType.exCardPackageChange));//��װ��֪ͨUI������
                    DataManager.get().load(true);


                    Toast.makeText(context, R.string.ypk_installed, Toast.LENGTH_LONG).show();

                    LogUtil.i("webCrawler", "Ex-card package is installed");

                    /* ���δ�������п����ã�����ת������ҳ�� */
                    if (!AppsSettings.get().isReadExpansions()) {//��ѹ��ϣ�����ʱ
                        LogUtil.i("webCrawler", "Ex-card setting is not opened");
                        Intent startSetting = new Intent(context, MainActivity.class);
                        startSetting.putExtra("flag", 4);
                        startActivity(startSetting);
                        Toast.makeText(context, R.string.ypk_go_setting, Toast.LENGTH_LONG).show();
                    }

                    break;
                case UnzipUtils.ZIP_UNZIP_EXCEPTION:
                    Toast.makeText(context, getString(R.string.install_failed_bcos) + msg.obj,
                            Toast.LENGTH_SHORT).show();
                    break;
//                case HomeFragment.TYPE_GET_DATA_VER_OK:
//                    WebActivity.exCardVer = msg.obj.toString();
//                    String oldVer = SharedPreferenceUtil.getExpansionDataVer();
//                    if (!TextUtils.isEmpty(WebActivity.exCardVer)) {
//                        if (!WebActivity.exCardVer.equals(oldVer)) {
//                            //btn_downloadչʾĬ����ͼ
//                        } else {
//                            btnDownload.setText(R.string.tip_redownload);
//                        }
//                    } else {
//                        showExNew();
//                    }
            }
        }
    };

    /**
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageReceived(ExCardEvent event) {
        if (event.getType() == ExCardEvent.EventType.exCardPackageChange) {
            changeDownloadText();
        }
    }
    private void downloadfromWeb(String fileUrl) {
        textDownload.setText("0%");//������غ󣬾���onDownloading����Ҫ�ȼ��룬��һ�ӳٻ���������Ӧ���Ĵ������������غ�����ʼ�������ı�
        File file = new File(AppsSettings.get().getResourcePath() + "-preRlease.zip");
        if (file.exists()) {
            FileUtils.deleteFile(file);
        }
        DownloadUtil.get().download(fileUrl, file.getParent(), file.getName(), new DownloadUtil.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File file) {
                downloadState = DownloadState.NO_DOWNLOAD;
                Message message = new Message();
                message.what = UnzipUtils.ZIP_READY;
                try {
                    File ydks = new File(AppsSettings.get().getDeckDir());
                    File[] subYdks = ydks.listFiles();
                    for (File files : subYdks) {
                        if (files.getName().contains("-") && files.getName().contains(" new cards"))
                            files.delete();
                    }
                    UnzipUtils.upZipSelectFile(file, AppsSettings.get().getResourcePath(), ".ypk");
                } catch (Exception e) {
                    message.what = UnzipUtils.ZIP_UNZIP_EXCEPTION;
                } finally {
                    message.what = UnzipUtils.ZIP_UNZIP_OK;//TODO ���԰ɣ�finally��һ��ִ�У�������ʹ��exceptionҲ�ᷢunzip_ok��
                }
                handler.sendMessage(message);
            }


            @Override
            public void onDownloading(int progress) {
                Message message = new Message();
                message.what = DownloadUtil.TYPE_DOWNLOAD_ING;
                message.arg1 = progress;
                handler.sendMessage(message);
            }

            @Override
            public void onDownloadFailed(Exception e) {
                //����ʧ�ܺ�ɾ�����ص��ļ�
                FileUtils.deleteFile(file);
                Message message = new Message();
                message.what = TYPE_DOWNLOAD_EXCEPTION;
                message.obj = e.toString();
                handler.sendMessage(message);
            }
        });

    }


}