import Vue from 'vue';
import App from './App.vue';
import './use';
import store from './store';
import VueNativeSock from 'vue-native-websocket';

Vue.use(VueNativeSock, 'ws://localhost:80/hi', {
  store: store,
  format: 'json',
  reconnection: true,
  reconnectionAttempts: 100,
  reconnectionDelay: 3000,
  connectManually: true
});
Vue.config.productionTip = false;

new Vue({
  render: h => h(App),
  store: store
}).$mount('#app');
