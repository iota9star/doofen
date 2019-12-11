/**
 * 该文件是为了按需加载，剔除掉了一些不需要的框架组件。
 * 减少了编译支持库包大小
 *
 * 当需要更多组件依赖时，在该文件加入即可
 */
import Vue from 'vue';
import {
  Button,
  Card,
  Col,
  Divider,
  Icon,
  Input,
  InputNumber,
  List,
  LocaleProvider,
  message,
  Modal,
  Pagination,
  Popover,
  Row,
  Select,
  Statistic,
  Table
} from 'ant-design-vue';

Vue.use(LocaleProvider);
Vue.use(Pagination);
Vue.use(Input);
Vue.use(InputNumber);
Vue.use(Button);
Vue.use(Divider);
Vue.use(Select);
Vue.use(Card);
Vue.use(Modal);
Vue.use(Table);
Vue.use(Icon);
Vue.use(Popover);
Vue.use(List);
Vue.use(Statistic);
Vue.use(Row);
Vue.use(Col);

Vue.prototype.$message = message;
