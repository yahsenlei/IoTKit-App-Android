package com.cylan.jiafeigou.n.view.login;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.misc.CheckServerTrustedWebViewClient;
import com.cylan.jiafeigou.utils.ActivityUtils;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.widget.CustomToolbar;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by cylan-hunt on 16-10-24.
 */

public class AgreementFragment extends Fragment {
    @BindView(R.id.webView)
    WebView webview;
    @BindView(R.id.custom_toolbar)
    CustomToolbar customToolbar;

    public static AgreementFragment getInstance(Bundle bundle) {
        AgreementFragment fragment = new AgreementFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agreement, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        customToolbar.setTvToolbarIcon(R.drawable.nav_icon_back_gary);
        customToolbar.setToolbarTitle(R.string.TERM_OF_USE);
        customToolbar.setBackAction((View v) -> {
            ActivityUtils.justPop(getActivity());
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        loadWeb();
    }

    private void loadWeb() {
        //用户协议
        final String agreementUrl = getString(R.string.Treaty_url,
                getString(R.string.agreementSuffix));
        WebSettings settings = webview.getSettings();
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);
        settings.setSavePassword(false);
        webview.removeJavascriptInterface("searchBoxJavaBridge_");
        webview.removeJavascriptInterface("accessibilityTraversal");
        webview.removeJavascriptInterface("accessibility");
        try {
            webview.setWebViewClient(new CheckServerTrustedWebViewClient(ContextUtils.getContext()) {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }

            });
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        webview.loadUrl(agreementUrl);
    }
}
