/**
 * Sample React Native App
 *
 * adapted from App.js generated by the following command:
 *
 * react-native init example
 *
 * https://github.com/facebook/react-native
 */

import React, {Component} from 'react';
import {StyleSheet, Text, SafeAreaView} from 'react-native';
import ByronKlineChart, {
  dispatchByronKline,
  KLineIndicator,
} from 'react-native-kline';
import axios from 'axios';

export default class App extends Component {
  state = {
    datas: [],
  };

  onMoreKLineData = async (params) => {
    console.log(' >> onMoreKLineData :',params);
    const res = await axios.get(
      'http://api.zhuwenbo.cc/v1/kline?type=MIN_30&symbol=btcusdt&to=' +
        params.id,
    );
    if (!res || !res.data) {
      return;
    }
    dispatchByronKline('add', res.data);
  };

  async initKlineChart() {
    const res = await axios.get(
      'http://api.zhuwenbo.cc/v1/kline?type=MIN_30&symbol=btcusdt',
    );
    if (!res || !res.data) {
      return;
    }
    this.setState({datas: res.data});
  }

  componentDidMount() {
    this.initKlineChart();
    const ws = new WebSocket('ws://49.233.210.12:1998/websocket');
    ws.onopen = () => {
      ws.send(
        JSON.stringify({
          event: 'subscribe',
          data: 'MIN_30/BTCUSDT',
        }),
      );
    };
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data);
        if (!msg || !this.state.datas.length) {
          return;
        }
        if (msg.type !== 'MIN_30/BTCUSDT') {
          return;
        }
        dispatchByronKline('update', [msg.data]);
      } catch (e) {}
    };
  }

  render() {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.welcome}>☆ByronKline example☆</Text>
        <Text style={styles.instructions}>STATUS: loaded</Text>
        <Text style={styles.welcome}>☆☆☆</Text>
        <ByronKlineChart
          style={{height: 400}}
          datas={this.state.datas}
          onMoreKLineData={this.onMoreKLineData}
          indicators={[]}
        />
      </SafeAreaView>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    // justifyContent: 'center',
    // alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
