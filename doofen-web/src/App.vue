<template>
  <a-locale-provider :locale="locale">
    <div class="main-wrapper">
      <div class="content-wrapper">
        <div class="header">
          <div class="logo">
            <img src="~@/assets/doofen.png" alt="多分">
          </div>
        </div>
        <a-card class="content">
          <div class="form">
            <label>学校</label>
            <a-select
              class="form-item"
              v-model="formBody.school"
              showSearch
              placeholder="请输入或选择学校"
              :filterOption="filterOption">
              <a-select-option :value="school._id" v-for="school in schools" :key="school._id">
                {{school.schName}}
              </a-select-option>
            </a-select>
            <label>年级</label>
            <a-input-number class="form-item small" :min="1" :max="100000" v-model="formBody.grade"/>
            <label>学号起始</label>
            <a-input-number class="form-item small" :min="1" :max="100000" v-model="formBody.from"/>
            <label>学号结束</label>
            <a-input-number class="form-item small" :min="1" :max="100000" v-model="formBody.to"/>
            <a-button @click="handleOk" type="primary" style="margin-right: 8px">查询</a-button>
            <a-button @click="resetFormBody" type="dashed">重置</a-button>
          </div>
          <div v-if="examName" class="exam-info">
            <label class="title">{{examName}}</label>
            <a-row :gutter="16">
              <a-col :span="6">
                <a-card>
                  <a-statistic
                    title="年级"
                    :value="currentGrade">
                  </a-statistic>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card>
                  <a-statistic
                    title="学生人数"
                    :value="gradeStuNum">
                  </a-statistic>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card>
                  <a-statistic
                    title="平均分/总分"
                    :value="gradeAvgScore+'/'+paperScore">
                  </a-statistic>
                </a-card>
              </a-col>
              <a-col :span="6" v-if="top&&typeof top.gradeTopScore ==='number'">
                <a-card>
                  <a-statistic
                    title="最高分/总分"
                    :value="top.gradeTopScore+'/'+paperScore">
                  </a-statistic>
                </a-card>
              </a-col>
              <a-col :span="6" v-for="exam in exams" :key="exam.xkId">
                <a-card>
                  <a-statistic
                    :title="exam.xkName"
                    :value="exam.gradeAvgScore+'/'+exam.paperScore">
                  </a-statistic>
                </a-card>
              </a-col>
            </a-row>
          </div>
          <a-table
            rowKey="stuId"
            :columns="columns"
            :dataSource="list"
            :key="exams.length+'_table_key'"
            :loading="dataLoading"
            :pagination="pagination"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            class="table-list"
            @change="handleTableChange"
            size="small">
            <template slot="stuId" slot-scope="stuId, record"><a href="javascript:" @click="showDetail(record)">{{stuId}}</a>
            </template>
            <template slot="stuScore" slot-scope="value">{{getValue(value)}}</template>
            <template slot="stuGradeRank" slot-scope="value">{{getValue(value)}}</template>
            <template slot="stuClassRank" slot-scope="value">{{getValue(value)}}</template>
            <template v-for="(exam,ind) in exams" :slot="exam._id" slot-scope="_, record">
              {{getScoreFromRecord(record,ind)}}
            </template>
            <template slot="expandedRowRender" slot-scope="record">
              <template v-if="record._id">
                <detail-list :title="'总览 '+record.stuScore+'/'+record.paperScore" style="margin-top: 16px">
                  <detail-list-item term="班级人数">{{record.classStuNum}}</detail-list-item>
                  <detail-list-item term="超越本班">{{record.clsPr}}%</detail-list-item>
                  <detail-list-item term="年级人数">{{record.gradeStuNum}}</detail-list-item>
                  <detail-list-item term="超越年级">{{record.gradePr}}%</detail-list-item>
                  <detail-list-item term="班级平均分">{{record.classAvgScore}}</detail-list-item>
                  <detail-list-item term="班级排名">{{record.stuClassRank}}</detail-list-item>
                  <detail-list-item term="年级平均分">{{record.gradeAvgScore}}</detail-list-item>
                  <detail-list-item term="年级排名">{{record.stuGradeRank}}</detail-list-item>
                </detail-list>
                <a-divider></a-divider>
                <detail-list
                  :title="exam.xkName+' '+exam.stuScore+'/'+exam.paperScore"
                  :key="exam.xkId"
                  v-for="exam in (record.exams || [])">
                  <detail-list-item term="班级平均分">{{exam.classAvgScore}}</detail-list-item>
                  <detail-list-item term="班级排名">{{exam.stuClassRank}}</detail-list-item>
                  <detail-list-item term="年级平均分">{{exam.gradeAvgScore}}</detail-list-item>
                  <detail-list-item term="年级排名">{{exam.stuGradeRank}}</detail-list-item>
                </detail-list>
              </template>
              <div v-else style="padding: 24px;text-align: center">
                暂无数据，可能未参考
              </div>
            </template>
          </a-table>
        </a-card>
      </div>
      <a-modal :title="'学号：'+record.stuId" v-model="visible" :footer="null" @close="visible=false" :width="720">
        <detail-list :title="'总览 '+record.stuScore+'/'+record.paperScore">
          <detail-list-item term="班级人数">{{record.classStuNum}}</detail-list-item>
          <detail-list-item term="超越本班">{{record.clsPr}}%</detail-list-item>
          <detail-list-item term="年级人数">{{record.gradeStuNum}}</detail-list-item>
          <detail-list-item term="超越年级">{{record.gradePr}}%</detail-list-item>
          <detail-list-item term="班级平均分">{{record.classAvgScore}}</detail-list-item>
          <detail-list-item term="班级排名">{{record.stuClassRank}}</detail-list-item>
          <detail-list-item term="年级平均分">{{record.gradeAvgScore}}</detail-list-item>
          <detail-list-item term="年级排名">{{record.stuGradeRank}}</detail-list-item>
        </detail-list>
        <a-divider></a-divider>
        <detail-list
          :title="exam.xkName+' '+exam.stuScore+'/'+exam.paperScore"
          :key="exam.xkId"
          v-for="exam in (record.exams || [])">
          <detail-list-item term="班级平均分">{{exam.classAvgScore}}</detail-list-item>
          <detail-list-item term="班级排名">{{exam.stuClassRank}}</detail-list-item>
          <detail-list-item term="年级平均分">{{exam.gradeAvgScore}}</detail-list-item>
          <detail-list-item term="年级排名">{{exam.stuGradeRank}}</detail-list-item>
        </detail-list>
      </a-modal>
    </div>
  </a-locale-provider>
</template>
<script>
    import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN';
    import DetailList from './DetailListItem';
    import { schools } from './school';

    const DetailListItem = DetailList.Item;
    export default {
        name: 'App',
        components: { DetailList, DetailListItem },
        data() {
            return {
                locale: zhCN,
                dataLoading: false,
                visible: false,
                schools: schools,
                grade: {
                    1: '一年级',
                    2: '二年级',
                    3: '三年级',
                    4: '四年级',
                    5: '五年级',
                    6: '六年级',
                    7: '七年级',
                    8: '八年级',
                    9: '九年级',
                    10: '高一年级',
                    11: '高二年级',
                    12: '高三年级'
                },
                list: [],
                cacheList: [],
                record: {},
                top: {},
                pagination: {
                    pageSizeOptions: ['10', '20', '30', '40', '50', '100', '250', '500', '1000'],
                    current: 1, // 返回结果中的当前分页数
                    total: 0, // 返回结果中的总记录数
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total, range) => `当前 ${range[0]} - ${range[1]} 共 ${total} 条`,
                    size: 'small',
                    pageSize: 10
                },
                formBody: {
                    from: 1,
                    to: 685,
                    grade: '19',
                    school: '280301'
                },
                lastFormBody: {},
                selectedRowKeys: []
            };
        },
        watch: {
            isConnected(val) {
              if (!val) this.$message.warn('连接已断开');
            }
        },
        created() {
            this.connect();
            this.$options.sockets.onmessage = (data) => {
                if (data.data) {
                    try {
                        const parsed = JSON.parse(data.data);
                        if (parsed.type === 1) {
                            const dat = parsed.data;
                            this.cacheList.push(dat);
                            if (dat && dat.examId && typeof (this.top || {}).gradeTopScore !== 'number') {
                                this.getTop(this.lastFormBody.school, dat.examId);
                            }
                        } else if (parsed.type === 0) {
                            this.$message.info(parsed.msg);
                        }
                    } catch (e) {
                        this.$message.error(e.toString());
                    }
                }
            };
        },
        beforeDestroy() {
            this.disconnect();
        },
        methods: {
            connect() {
                if (!this.isConnected) {
                    this.$connect();
                }
            },
            disconnect() {
                delete this.$options.sockets.onmessage;
                this.$disconnect();
            },
            getTop(school, examId) {
                fetch(`/top/${school}/${examId}`)
                    .then((resp) => resp.json())
                    .then((json) => {
                        if (json && json.data) {
                            this.top = json.data;
                        }
                    })
                    .catch((e) => {
                        console.log(e);
                    });
            },
            startInterval() {
                this.timer = setInterval(() => {
                    if (this.list && this.cacheList && this.list.length < this.cacheList.length) {
                        this.list = [...this.cacheList];
                    }
                    if (this.lastFormBody.to && this.lastFormBody.from && this.list.length === this.lastFormBody.to - this.lastFormBody.from + 1) {
                        this.stopInterval();
                    }
                }, 3000);
            },
            stopInterval() {
                if (this.timer) {
                    clearInterval(this.timer);
                    this.timer = null;
                }
            },
            filterOption(input, option) {
                return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0;
            },
            showDetail(record) {
                if (!record._id) {
                    return this.$message.info('当前学生没有数据');
                }
                this.record = record;
                this.visible = true;
            },
            getValue(score) {
                if (typeof score === 'number') return score;
                return '---';
            },
            getScoreFromRecord(record, ind) {
                const exams = record.exams || [];
                if (exams.length > ind) {
                    return exams[ind].stuScore;
                }
                return '---';
            },
            resetFormBody() {
                this.formBody = {
                    from: 1,
                    to: 685,
                    grade: '19',
                    school: '280301'
                };
            },
            handleOk() {
                this.cacheList = [];
                this.list = [];
                this.top = {};
                this.stopInterval();
                this.startInterval();
                this.lastFormBody = Object.assign({}, this.formBody);
                if (this.isConnected) {
                    console.log('发送：', this.lastFormBody);
                    this.$socket.sendObj(this.lastFormBody);
                }
            },
            onSelectChange(selectedRowKeys) {
                this.selectedRowKeys = selectedRowKeys;
            },
            handleTableChange(pagination) {
                this.pagination.current = pagination.current;
                this.pagination.pageSize = pagination.pageSize;
            }
        },
        computed: {
            isConnected() {
                return this.$store.state.socket.isConnected === true;
            },
            columns() {
                const columns = [
                    {
                        title: '学号',
                        dataIndex: 'stuId',
                        align: 'center',
                        scopedSlots: { customRender: 'stuId' },
                        sorter: (ra, rb) => {
                            const a = ra.stuId;
                            const b = rb.stuId;
                            return a - b;
                        },
                        sortDirections: ['descend', 'ascend']
                    },
                    {
                        title: '总分',
                        dataIndex: 'stuScore',
                        scopedSlots: { customRender: 'stuScore' },
                        align: 'center',
                        sorter: (ra, rb) => {
                            const a = ra.stuScore;
                            const b = rb.stuScore;
                            if (typeof a !== 'number') {
                                return -1;
                            }
                            if (typeof b !== 'number') {
                                return 1;
                            }
                            return a - b;
                        },
                        sortDirections: ['descend', 'ascend']
                    },
                    {
                        title: '年级排名',
                        dataIndex: 'stuGradeRank',
                        scopedSlots: { customRender: 'stuGradeRank' },
                        align: 'center',
                        sorter: (ra, rb) => {
                            const a = ra.stuGradeRank;
                            const b = rb.stuGradeRank;
                            if (typeof a !== 'number') {
                                return -1;
                            }
                            if (typeof b !== 'number') {
                                return 1;
                            }
                            return a - b;
                        },
                        sortDirections: ['descend', 'ascend']
                    },
                    {
                        title: '班级排名',
                        dataIndex: 'stuClassRank',
                        scopedSlots: { customRender: 'stuClassRank' },
                        align: 'center',
                        sorter: (ra, rb) => {
                            const a = ra.stuClassRank;
                            const b = rb.stuClassRank;
                            if (typeof a !== 'number') {
                                return -1;
                            }
                            if (typeof b !== 'number') {
                                return 1;
                            }
                            return a - b;
                        },
                        sortDirections: ['descend', 'ascend']
                    }
                ];
                if (this.exams && this.exams.length > 0) {
                    let exam;
                    for (let i = 0; i < this.exams.length; i++) {
                        exam = this.exams[i];
                        columns.push({
                            title: exam.xkName,
                            scopedSlots: { customRender: exam._id },
                            align: 'center',
                            key: exam._id,
                            exam: true,
                            sorter: (ra, rb) => {
                                const a = this.getScoreFromRecord(ra, i);
                                const b = this.getScoreFromRecord(rb, i);
                                if (typeof a !== 'number') {
                                    return -1;
                                }
                                if (typeof b !== 'number') {
                                    return 1;
                                }
                                return a - b;
                            },
                            sortDirections: ['descend', 'ascend']
                        });
                    }
                }
                return columns;
            },
            gradeStuNum() { // 年级学生数
                const find = this.cacheList.find(it => typeof it.gradeStuNum === 'number');
                if (find) {
                    return find.gradeStuNum;
                }
                return '暂无数据';
            },
            gradeAvgScore() { // 年级平均分
                const find = this.cacheList.find(it => typeof it.gradeAvgScore === 'number');
                if (find) {
                    return find.gradeAvgScore;
                }
                return '暂无数据';
            },
            paperScore() { // 卷面总分
                const find = this.cacheList.find(it => typeof it.paperScore === 'number');
                if (find) {
                    return find.paperScore;
                }
                return '暂无数据';
            },
            currentGrade() { // 年级
                const find = this.cacheList.find(it => typeof it.examGradeId === 'number');
                if (find) {
                    return this.grade[find.examGradeId];
                }
                return '暂无数据';
            },
            examName() { // 考试名称
                const find = this.cacheList.find(it => it.examName);
                if (find) {
                    return (find.examName || '').replace(/_/g, ' ');
                }
                return null;
            },
            exams() {
                if (this.cacheList && this.cacheList.length > 0) {
                    let max = 0;
                    let index = 0;
                    this.cacheList.forEach((v, i) => {
                        if (v.exams) {
                            const length = v.exams.length || 0;
                            if (length > max) {
                                max = length;
                                index = i;
                            }
                        }
                    });
                    return this.cacheList[index].exams;
                }
                return [];
            }
        }
    };
</script>
<style lang="less">
  * {
    margin: 0;
    padding: 0;
  }

  .main-wrapper {
    background: #181759 url('~@/assets/background.svg') no-repeat 50%;
    background-size: 100%;
    position: fixed;
    left: 0;
    top: 0;
    overflow-y: auto;
    overflow-x: hidden;
    bottom: 0;
    right: 0;

    .content-wrapper {
      min-height: 100%;
      width: 100%;
      padding-bottom: 96px;
      box-sizing: border-box;
      position: relative;

      .header {
        margin: 48px 0 36px;
        text-align: center;
        font-size: 20px;
        font-weight: bold;
      }

      .content {
        width: 90%;
        min-width: 720px;
        margin: 0 auto;

        .exam-info {
          .ant-card {
            margin-bottom: 16px;

            .ant-card-body {
              padding: 14px;

              .ant-statistic-content {
                line-height: 1;

                .ant-statistic-content-value {
                  font-size: 18px;
                }
              }
            }
          }

          .title {
            display: block;
            font-size: 16px;
            padding: 12px 0;
          }
        }

        .form {
          display: flex;
          flex-wrap: wrap;
          align-items: center;
          margin-bottom: 16px;

          > * {
            margin-top: 12px;
          }

          .form-item {
            width: 300px;
            margin-left: 10px;
            margin-right: 16px;

            &.small {
              width: 64px;
            }
          }
        }
      }
    }
  }
</style>
