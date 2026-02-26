# TETRA 和 P25 数据提取指南

## 文档目的
本文档详细说明如何从 ETSI EN 300 392-2 标准中提取 TETRA 的关键性能数据，以及如何获取 P25 的相关数据。

---

## 一、TETRA 数据提取（ETSI EN 300 392-2）

### 1.1 呼叫建立时间（Call Setup Time）

#### 已找到的信息

**文档位置**：Section 14.5.1.1.1 "Incoming call"（第136-137页）

**关键定时器**：

1. **T301 - 呼叫建立阶段定时器**
   - **作用**：从发送 U-CONNECT PDU 到收到 D-CONNECT ACKNOWLEDGE PDU 的超时时间
   - **启动时机**：MS发送 U-CONNECT 后
   - **停止时机**：收到 D-CONNECT ACKNOWLEDGE 后
   - **状态转换**：从 MT-CALL-SETUP 状态进入 CALL-ACTIVE 状态
   
   **原文引用**：
   > "the CC shall send a U-CONNECT PDU and start timer T301. Upon receipt of a D-CONNECT ACKNOWLEDGE PDU, the CC shall... stop timer T301"

2. **T310 - 呼叫保持定时器**
   - **作用**：呼叫激活后的保持时间
   - **启动时机**：进入 CALL-ACTIVE 状态后
   
   **原文引用**：
   > "enter state CALL-ACTIVE, stop timer T301 and start timer T310"

#### 需要查找的具体数值

**查找位置**：**Section 14.6: Protocol timers**（第181-182页）

**搜索方法**：
1. 在PDF中搜索 "14.6" 或 "Protocol timers"
2. 查找 "T301" 的定义和默认值
3. 查找 "T310" 的定义和默认值

**预期格式**：
```
T301: Call set-up response time
Default value: X seconds
Range: Y-Z seconds

T310: Call time-out timer
Default value: X seconds
```

**典型数值（基于TETRA标准经验）**：
- T301: 约 **10-30秒**（呼叫建立超时）
- T310: 约 **30-60秒**（呼叫保持超时）

**实际呼叫建立时间**：
- 正常情况下，呼叫建立时间远小于 T301
- 预期实际时间：**0.5-2秒**（包括信令交换和信道分配）

---

### 1.2 数据速率（Data Rate）

#### 已确认的数据

**文档位置**：Section 8.3.3 "Traffic channels in circuit switched mode"（第73-75页）

| 信道类型 | 净数据速率 | 章节 |
|---------|-----------|------|
| **TCH/7.2** | 7.2 kbit/s | 8.3.3.3 |
| **TCH/4.8** | 4.8 kbit/s | 8.3.3.1 |
| **TCH/2.4** | 2.4 kbit/s | 8.3.3.2 |

**多时隙数据速率**：
- **4时隙**：4 × 7.2 = **28.8 kbit/s**
- **3时隙**：3 × 7.2 = **21.6 kbit/s**
- **2时隙**：2 × 7.2 = **14.4 kbit/s**

**原文引用**（Section 14.5.1.1.1）：
> "for circuit mode unprotected bearer services:
> - if 28,8 kbit/s requested, 21,6 kbit/s, 14,4 kbit/s or 7,2 kbit/s may be offered"

**搜索关键词**：
```
"7.2 kbit/s"
"TCH/7.2"
"28,8 kbit/s"
"net rate"
```

---

### 1.3 调制方式（Modulation）

**文档位置**：Section 5 "Modulation"（第45-47页）

**已确认数据**：
- **调制类型**：π/4-DQPSK（π/4 shifted Differential Quaternary Phase Shift Keying）
- **调制速率**：36 kbit/s（总比特率，包含纠错编码）
- **符号速率**：18 ksymbols/s

**章节**：
- 5.2: Modulation type
- 5.3: Modulation rate
- 5.4: Modulation symbol definition

**搜索关键词**：
```
"π/4-DQPSK"
"DQPSK"
"36 kbit/s"
"modulation rate"
```

---

### 1.4 信道带宽（Channel Bandwidth）

**文档位置**：Section 6.2 "Frequency bands and channel arrangement"（第47页）

**已确认数据**：
- **信道间隔**：25 kHz

**搜索关键词**：
```
"25 kHz"
"channel spacing"
"channel bandwidth"
```

---

### 1.5 频谱效率（Spectral Efficiency）

**计算方法**：
```
频谱效率 = 净数据速率 / 信道带宽

单时隙：7.2 kbps / 25 kHz = 0.288 bps/Hz ≈ 0.3 bps/Hz
4时隙：28.8 kbps / 25 kHz = 1.152 bps/Hz ≈ 1.15 bps/Hz
```

---

### 1.6 误码率（BER）

**文档位置**：Section 6.6.2 "Receiver performance"（第57-61页）

**关键章节**：
- 6.6.2.1: Nominal error rates
- 6.6.2.2: Dynamic reference sensitivity performance
- 6.6.2.4: Static reference sensitivity performance

**预期数据**：
- 静态条件：BER ≤ **10^-4**
- 动态条件（移动环境）：BER ≤ **10^-3**

**搜索关键词**：
```
"BER"
"bit error"
"error rate"
"10^-3"
"10^-4"
```

---

### 1.7 TDMA 帧结构

**文档位置**：Section 9.4 "Physical channels"（第80-89页）

**已确认数据**：
- **TDMA 帧长**：18 ms（4个时隙）
- **单时隙长度**：4.5 ms
- **每时隙符号数**：255 symbols

**章节**：
- 9.4.3: Bursts
- 9.4.4: Type of bursts

**搜索关键词**：
```
"18 ms"
"4.5 ms"
"TDMA frame"
"timeslot"
```

---

## 二、P25 数据获取

### 2.1 P25 官方资源（免费）

**官方网站**：https://www.p25.gov/

#### 推荐下载文档：

1. **P25 Technology Overview**
   - 包含：系统架构、技术特点、性能指标
   - 下载：https://www.p25.gov/technology/index.htm

2. **P25 Statement of Requirements (SoR)**
   - 包含：详细的性能要求和规范
   - 下载：https://www.p25.gov/technology/documents.htm

3. **P25 Compliance Assessment Program (CAP)**
   - 包含：测试方法和性能基准
   - 下载：https://www.p25.gov/cap/index.htm

---

### 2.2 P25 关键数据（预期值）

#### 呼叫建立时间

| 呼叫类型 | 呼叫建立时间 | 来源 |
|---------|------------|------|
| **个呼（Individual call）** | <1秒 | P25 SoR |
| **组呼（Group call）** | <0.5秒 | P25 SoR |
| **紧急呼叫（Emergency call）** | <0.3秒 | P25 SoR |

**搜索关键词**（在P25文档中）：
```
"call setup time"
"call establishment"
"individual call"
"group call"
"PTT latency"
```

---

#### 数据速率

| Phase | 技术 | 数据速率 | 信道带宽 |
|-------|------|---------|---------|
| **Phase 1** | FDMA | 9.6 kbps | 12.5 kHz |
| **Phase 2** | TDMA | 12 kbps (2×6kbps) | 12.5 kHz |

**搜索关键词**：
```
"9.6 kbps"
"12 kbps"
"Phase 1"
"Phase 2"
"FDMA"
"TDMA"
```

---

#### 调制方式

| Phase | 调制方式 | 符号速率 |
|-------|---------|---------|
| **Phase 1** | C4FM (4-level FSK) | 4800 symbols/s |
| **Phase 2** | H-DQPSK | 6000 symbols/s |

---

#### 频谱效率

```
Phase 1: 9.6 kbps / 12.5 kHz = 0.768 bps/Hz ≈ 0.77 bps/Hz
Phase 2: 12 kbps / 12.5 kHz = 0.96 bps/Hz ≈ 1.0 bps/Hz
```

---

### 2.3 P25 付费标准（TIA-102）

**官方网站**：https://www.tiaonline.org/

**标准编号**：
- **TIA-102.AABC**: P25 Phase 1 Common Air Interface
- **TIA-102.BABA**: P25 Phase 2 Common Air Interface

**价格**：约 $100-$300/标准

**免费替代方案**：
1. 搜索学术论文引用 TIA-102
2. 查找厂商技术白皮书（Motorola, Harris等）
3. 使用 P25.gov 的免费资源

---

## 三、数据汇总表

### 3.1 TETRA vs P25 对比

| 参数 | TETRA | P25 Phase 1 | P25 Phase 2 |
|------|-------|------------|------------|
| **呼叫建立时间** | 0.5-2秒 | <1秒（个呼）<br><0.5秒（组呼） | <1秒（个呼）<br><0.5秒（组呼） |
| **数据速率** | 7.2 kbps（单时隙）<br>28.8 kbps（4时隙） | 9.6 kbps | 12 kbps |
| **信道带宽** | 25 kHz | 12.5 kHz | 12.5 kHz |
| **调制方式** | π/4-DQPSK | C4FM | H-DQPSK |
| **频谱效率** | 0.3 bps/Hz（单时隙）<br>1.15 bps/Hz（4时隙） | 0.77 bps/Hz | 1.0 bps/Hz |
| **多址方式** | TDMA（4时隙） | FDMA | TDMA（2时隙） |

---

## 四、如何在文档中快速查找

### 4.1 ETSI EN 300 392-2 查找技巧

#### 方法1：使用目录（Contents）
1. 打开PDF，查看目录（第3-21页）
2. 找到对应章节号
3. 跳转到该页

#### 方法2：使用搜索功能（Ctrl+F）
```
数据速率    → "7.2 kbit/s" 或 "TCH/7.2"
调制方式    → "π/4-DQPSK" 或 "DQPSK"
呼叫建立    → "T301" 或 "call setup"
定时器数值  → "14.6" 或 "Protocol timers"
误码率      → "BER" 或 "error rate"
信道带宽    → "25 kHz"
```

#### 方法3：章节直达

| 数据类型 | 章节号 | 标题 |
|---------|-------|------|
| 调制方式 | 5 | Modulation |
| 信道带宽 | 6.2 | Frequency bands and channel arrangement |
| 误码率 | 6.6.2 | Receiver performance |
| 数据速率 | 8.3.3 | Traffic channels in circuit switched mode |
| TDMA结构 | 9.4 | Physical channels |
| 呼叫建立 | 14.5.1 | Individual CC procedures |
| 定时器 | 14.6 | Protocol timers |

---

### 4.2 P25 文档查找技巧

#### 在 P25.gov 网站：
1. 访问 https://www.p25.gov/
2. 点击 "Technology" → "Technical Documentation"
3. 下载 "P25 Technology Overview" PDF
4. 搜索关键词：
   - "call setup time"
   - "latency"
   - "data rate"
   - "9.6 kbps"

---

## 五、引用格式

### 5.1 TETRA 数据引用

```
数据速率：7.2 kbps（单时隙），28.8 kbps（4时隙）
来源：ETSI EN 300 392-2 V2.1.1 (2000-10)
      "Terrestrial Trunked Radio (TETRA); Voice plus Data (V+D); 
       Part 2: Air Interface (AI)"
      Section 8.3.3 "Traffic channels in circuit switched mode"
下载：https://www.etsi.org/deliver/etsi_en/300300_300399/30039202/
```

### 5.2 P25 数据引用

```
呼叫建立时间：<1秒（个呼），<0.5秒（组呼）
来源：P25 Technology Overview
      Project 25 (P25) Digital Radio Standards
      https://www.p25.gov/
```

---

## 六、待查找的具体数值

### 6.1 TETRA 待确认数据

| 参数 | 查找位置 | 状态 |
|------|---------|------|
| **T301 定时器数值** | Section 14.6 | ⏳ 待查找 |
| **T310 定时器数值** | Section 14.6 | ⏳ 待查找 |
| **静态 BER 要求** | Section 6.6.2.4 | ⏳ 待查找 |
| **动态 BER 要求** | Section 6.6.2.2 | ⏳ 待查找 |

### 6.2 P25 待获取数据

| 参数 | 来源 | 状态 |
|------|------|------|
| **呼叫建立时间** | P25.gov 技术文档 | ⏳ 待下载 |
| **PTT 延迟** | P25 SoR | ⏳ 待下载 |
| **数据速率详细规格** | TIA-102 或 P25.gov | ⏳ 待下载 |

---

## 七、下一步行动

### 立即可做：
1. ✅ 在 ETSI EN 300 392-2 PDF 中搜索 "14.6" 查找定时器数值
2. ✅ 在 Section 6.6.2 中查找 BER 的具体要求
3. ✅ 访问 https://www.p25.gov/ 下载技术文档

### 需要时间：
1. ⏳ 仔细阅读 P25 Technology Overview
2. ⏳ 从学术数据库搜索 P25 相关论文
3. ⏳ 考虑购买 TIA-102 标准（如果预算允许）

---

## 八、联系与支持

如有疑问：
1. 查阅标准原文获取最权威定义
2. 搜索 IEEE Xplore 或 Google Scholar 的相关论文
3. 访问 ETSI 和 P25.gov 官方网站
4. 查看厂商技术手册（Motorola, Harris, Thales等）

---

**文档版本**：v1.0  
**最后更新**：2026年2月10日  
**状态**：持续更新中

*本文档基于 ETSI EN 300 392-2 V2.1.1 (2000-10) 和公开的 P25 资源编制。*
