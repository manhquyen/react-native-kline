package com.byron.kline;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.byron.kline.adapter.KLineChartAdapter;
import com.byron.kline.formatter.ValueFormatter;
import com.byron.kline.utils.SlidListener;
import com.byron.kline.utils.Status;
import com.byron.kline.view.KChartView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import org.jetbrains.annotations.NotNull;

import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.security.AccessController.getContext;


public class ByronKlineManager extends ViewGroupManager {

    private LinearLayout _mContainer;
    private ThemedReactContext _mContext;
    private KChartView _chartView;
    private KLineChartAdapter _adapter;
    private ReadableArray _datas;
    private int _pricePrecision = 2;
    private int _volumePrecision = 2;
    private ReadableArray _locales;
    private ReadableArray _indicators;
    private Boolean _requestStatus = false;
    private String _increaseColor = "#00BD9A";
    private String _decreaseColor = "#FF6960";

    public static final String REACT_CLASS = "ByronKline";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public View createViewInstance(ThemedReactContext context) {
        _mContext = context;
        @SuppressLint("InflateParams") View layout = LayoutInflater.from(context).inflate(R.layout.kline, null);
        layout.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        _mContainer = layout.findViewById(R.id.container);
        return layout;
    }

    @ReactProp(name = "datas")
    public void setDatas(View view, ReadableArray datas) {
        if (datas == null) {
            return;
        }
        _datas = datas;
        int size = _datas.toArrayList().size();
        if (size == 0) {
            return;
        }
        if (_chartView == null || _adapter == null) {
            return;
        }
        Gson gson = new Gson();
        String json = gson.toJson(_datas.toArrayList());
        List<KChartBean> list = gson.fromJson(
                json,
                new TypeToken<List<KChartBean>>() {
                }.getType()
        );
        _adapter.resetData(list, true);
    }

    @ReactProp(name = "locales")
    public void setLocales(View view, ReadableArray locales) {
        Log.d("setLocales", "value: " + locales);
        if (locales == null) {
            return;
        }
        _locales = locales;
        if (_chartView == null || _adapter == null) {
            return;
        }
        if (_locales.toArrayList().size() == 0) {
            return;
        }
        Gson gson = new Gson();
        String json = gson.toJson(_locales.toArrayList());
        String[] list = gson.fromJson(
                json,
                new TypeToken<String[]>() {
                }.getType()
        );
        _chartView.setSelectedInfoLabels(list);
    }

    @ReactProp(name = "indicators")
    public void setIndicators(View view, ReadableArray indicators) {
        Log.d("setIndicators", "value: " + indicators);
        if (indicators == null) {
            return;
        }
        _indicators = indicators;
        if (_chartView == null || _adapter == null) {
            return;
        }
        List list = _indicators.toArrayList();
        if (list.size() == 0) {
            initKLineState();
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Object status = list.get(i);
            changeKLineState(status);
        }
    }

    @ReactProp(name = "pricePrecision")
    public void setPricePrecision(View view, int pricePrecision) {
        _pricePrecision = pricePrecision;
        if (_chartView == null || _adapter == null) {
            return;
        }
        _chartView.setValueFormatter(
                new ValueFormatter() {
                    @Override
                    public String format(float value) {
                        return String.format(Locale.CHINA, "%." + _pricePrecision + "f", value);
                    }
                }
        );
    }

    @ReactProp(name = "volumePrecision")
    public void setVolumePrecision(View view, int volumePrecision) {
        _volumePrecision = volumePrecision;
        if (_chartView == null || _adapter == null) {
            return;
        }
        _chartView.setVolFormatter(
                new ValueFormatter() {
                    @Override
                    public String format(float value) {
                        return String.format(Locale.CHINA, "%." + _volumePrecision + "f", value);
                    }
                }
        );
    }

    @ReactProp(name = "increaseColor")
    public void setIncreaseColor(View view, String increaseColor) {
        _increaseColor = increaseColor;
        if (_chartView == null || _adapter == null) {
            return;
        }
        _chartView.setIncreaseColor(Color.parseColor(_increaseColor));
    }

    @ReactProp(name = "decreaseColor")
    public void setDecreaseColor(View view, String decreaseColor) {
        _decreaseColor = decreaseColor;
        if (_chartView == null || _adapter == null) {
            return;
        }
        _chartView.setDecreaseColor(Color.parseColor(_decreaseColor));
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "byronController", 1001
        );

    }

    @Override
    public void receiveCommand(@NotNull View view, int commandId, @Nullable ReadableArray args) {
        assert args != null;
        Gson gson = new Gson();
        List<Object> arrayList = args.toArrayList();
        String json = gson.toJson(arrayList.get(0));
        ByronController options = gson.fromJson(
                json,
                new TypeToken<ByronController>() {
                }.getType()
        );
        if (options.event.equals("init")) {
            initChartView();
        }
        if (options.event.equals("update") && _adapter != null) {
            if (_requestStatus) {
                return;
            }
            List<KChartBean> datas = _adapter.getDatas();
            if (datas.size() < 2) {
                return;
            }
            KChartBean bar = options.list.get(0);
            int count = datas.size();
            int last1 = count - 1;
            int last2 = count - 2;
            long differ = datas.get(last1).id - datas.get(last2).id;
            long limit = datas.get(last1).id + differ;
            if (bar.id < limit) {
                bar.id = datas.get(last1).id;
                _adapter.changeItem(last1, bar);
            } else {
                _adapter.addLast(bar);
            }
        }
        if (options.event.equals("add") && _adapter != null) {
            _adapter.addFooterData(options.list);
            if (_requestStatus) {
                _requestStatus = false;
            }
        }
    }

    @Override
    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder().put(
                "onMoreKLineData",
                MapBuilder.of(
                        "phasedRegistrationNames",
                        MapBuilder.of("bubbled", "onRNMoreKLineData"))
        ).build();
    }

    public void onReceiveNativeEvent() {
        List<KChartBean> list = _adapter.getDatas();
        WritableMap event = Arguments.createMap();
        event.putDouble("id", list.get(0).id);
        _mContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                _mContainer.getId(),
                "onMoreKLineData",
                event
        );
    }

    private void initChartView() {
        _adapter = new KLineChartAdapter();
        _mContainer.setVisibility(View.VISIBLE);
        _chartView = _mContainer.findViewById(R.id.kLineChartView);
        DisplayMetrics dm2 = _mContext.getResources().getDisplayMetrics();
        _chartView.setGridColumns(5).setGridRows(5).setOverScrollRange(dm2.widthPixels / 5);
        _chartView.setAdapter(_adapter);
        setIndicators(_chartView, _indicators);
        setVolumePrecision(_chartView, _volumePrecision);
        setPricePrecision(_chartView, _pricePrecision);
        setIncreaseColor(_chartView, _increaseColor);
        setDecreaseColor(_chartView, _decreaseColor);
        initKLineState();
        _chartView.setSlidListener(new SlidListener() {
            @Override
            public void onSlidLeft() {
                if (_requestStatus) {
                    return;
                }
                _requestStatus = true;
                onReceiveNativeEvent();
            }

            @Override
            public void onSlidRight() {

            }
        });
    }

    private void initKLineState() {
        if (_chartView == null) {
            return;
        }
        _chartView.hideSelectData();
        _chartView.changeMainDrawType(Status.MainStatus.NONE);
        _chartView.setIndexDraw(Status.IndexStatus.NONE);
        _chartView.setKlineState(Status.KlineStatus.K_LINE);
        _chartView.setVolShowState(false);
    }

    private void changeKLineState(Object status) {
        if (status == null) {
            return;
        }
        if ("0.0".equals(status)) { // 显示MA
            _chartView.hideSelectData();
            _chartView.changeMainDrawType(Status.MainStatus.MA);
        } else if ("1.0".equals(status)) {  // BOLL
            _chartView.hideSelectData();
            _chartView.changeMainDrawType(Status.MainStatus.BOLL);
        } else if ("2.0".equals(status)) { // MainStateNONE
            _chartView.hideSelectData();
            _chartView.changeMainDrawType(Status.MainStatus.NONE);
        } else if ("3.0".equals(status)) { // SecondaryStateMacd
            _chartView.hideSelectData();
            _chartView.setIndexDraw(Status.IndexStatus.MACD);
        } else if ("4.0".equals(status)) { // SecondaryStateKDJ
            _chartView.hideSelectData();
            _chartView.setIndexDraw(Status.IndexStatus.KDJ);
        } else if ("5.0".equals(status)) { // SecondaryStateRSI
            _chartView.hideSelectData();
            _chartView.setIndexDraw(Status.IndexStatus.RSI);
        } else if ("6.0".equals(status)) { // SecondaryStateWR
            _chartView.hideSelectData();
            _chartView.setIndexDraw(Status.IndexStatus.WR);
        } else if ("7.0".equals(status)) { // SecondaryStateNONE
            _chartView.hideSelectData();
            _chartView.setIndexDraw(Status.IndexStatus.NONE);
        } else if ("8.0".equals(status)) { // ShowLine 显示分时图
            _chartView.hideSelectData();
            _chartView.setKlineState(Status.KlineStatus.TIME_LINE);
        } else if ("9.0".equals(status)) { // HideLine 隐藏分时图
            _chartView.hideSelectData();
            _chartView.setKlineState(Status.KlineStatus.K_LINE);
        } else if ("10.0".equals(status)) { // 显示成交量
            _chartView.setVolShowState(true);
        } else if ("11.0".equals(status)) { // 隐藏成交量
            _chartView.setVolShowState(false);
        }
    }
}
