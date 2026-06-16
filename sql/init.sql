-- ============================================
-- 古代云锦织机提花工艺仿真与织物结构分析系统
-- PostgreSQL 数据库初始化脚本
-- ============================================

CREATE DATABASE yunjin_weaving;
\c yunjin_weaving;

-- 织机表
CREATE TABLE IF NOT EXISTS loom (
    id BIGSERIAL PRIMARY KEY,
    loom_code VARCHAR(50) UNIQUE NOT NULL,
    loom_name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    status VARCHAR(20) DEFAULT 'IDLE',
    total_warp_count INTEGER DEFAULT 1200,
    weft_density_target DOUBLE PRECISION DEFAULT 60.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 传感器数据表
CREATE TABLE IF NOT EXISTS sensor_data (
    id BIGSERIAL PRIMARY KEY,
    loom_id BIGINT NOT NULL REFERENCES loom(id) ON DELETE CASCADE,
    warp_tension DOUBLE PRECISION NOT NULL,
    weft_density DOUBLE PRECISION NOT NULL,
    pattern_position INTEGER NOT NULL,
    fabric_progress DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    warp_tension_array TEXT,
    shed_opening_array TEXT
);

CREATE INDEX IF NOT EXISTS idx_sensor_data_loom_id ON sensor_data(loom_id);
CREATE INDEX IF NOT EXISTS idx_sensor_data_timestamp ON sensor_data(timestamp);

-- 告警表
CREATE TABLE IF NOT EXISTS alert (
    id BIGSERIAL PRIMARY KEY,
    loom_id BIGINT NOT NULL REFERENCES loom(id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL,
    alert_level VARCHAR(20) DEFAULT 'WARNING',
    message TEXT,
    resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_loom_id ON alert(loom_id);
CREATE INDEX IF NOT EXISTS idx_alert_resolved ON alert(resolved);

-- 织物结构分析记录表
CREATE TABLE IF NOT EXISTS fabric_analysis (
    id BIGSERIAL PRIMARY KEY,
    loom_id BIGINT NOT NULL REFERENCES loom(id) ON DELETE CASCADE,
    analysis_type VARCHAR(50) NOT NULL,
    weave_pattern VARCHAR(50),
    warp_count INTEGER,
    weft_count INTEGER,
    texture_data TEXT,
    fft_spectrum TEXT,
    result_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fabric_analysis_loom_id ON fabric_analysis(loom_id);

-- 织造仿真状态表
CREATE TABLE IF NOT EXISTS weaving_simulation (
    id BIGSERIAL PRIMARY KEY,
    loom_id BIGINT UNIQUE NOT NULL REFERENCES loom(id) ON DELETE CASCADE,
    current_weft_row INTEGER DEFAULT 0,
    shed_state TEXT,
    interlacement_matrix TEXT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 复合索引：传感器数据按织机+时间范围查询
CREATE INDEX IF NOT EXISTS idx_sensor_data_loom_time ON sensor_data(loom_id, timestamp DESC);

-- 覆盖索引：活跃告警快速查询
CREATE INDEX IF NOT EXISTS idx_alert_active ON alert(resolved, created_at DESC) WHERE resolved = FALSE;

-- 织机状态查询索引
CREATE INDEX IF NOT EXISTS idx_alert_type_level ON alert(alert_type, alert_level);

-- 仿真状态查询
CREATE INDEX IF NOT EXISTS idx_weaving_simulation_loom ON weaving_simulation(loom_id);

-- 织物分析按时间倒序
CREATE INDEX IF NOT EXISTS idx_fabric_analysis_loom_time ON fabric_analysis(loom_id, created_at DESC);

-- 初始数据
INSERT INTO loom (loom_code, loom_name, location, status, total_warp_count, weft_density_target)
VALUES 
('YJ-001', '南京云锦大花楼织机一号', '纺织史研究实验室A区', 'IDLE', 1200, 60.0),
('YJ-002', '南京云锦大花楼织机二号', '纺织史研究实验室A区', 'IDLE', 1400, 58.0)
ON CONFLICT (loom_code) DO NOTHING;

-- ============================================================
-- 云锦品种表 (yunjin_variety)
-- ============================================================
CREATE TABLE IF NOT EXISTS yunjin_variety (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    alias VARCHAR(500),
    description VARCHAR(2000),
    dynasty VARCHAR(100),
    weave_type VARCHAR(50),
    warp_count INTEGER,
    weft_count INTEGER,
    warp_density_per_cm DOUBLE PRECISION,
    weft_density_per_cm DOUBLE PRECISION,
    color_count INTEGER,
    pattern_repeat_unit VARCHAR(50),
    pattern_repeat_warp INTEGER,
    pattern_repeat_weft INTEGER,
    shedding_mechanism VARCHAR(50),
    harness_count INTEGER,
    production_speed_cm_per_day DOUBLE PRECISION,
    raw_material VARCHAR(50),
    silk_weight_per_sqm_gram DOUBLE PRECISION,
    characteristics VARCHAR(2000),
    historical_usage VARCHAR(2000),
    cultural_significance VARCHAR(2000),
    palette_colors TEXT,
    complexity_score INTEGER,
    rarity_score DOUBLE PRECISION,
    representative_works VARCHAR(500),
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_yunjin_variety_code ON yunjin_variety(code);
CREATE INDEX IF NOT EXISTS idx_yunjin_variety_dynasty ON yunjin_variety(dynasty);
CREATE INDEX IF NOT EXISTS idx_yunjin_variety_weave_type ON yunjin_variety(weave_type);
CREATE INDEX IF NOT EXISTS idx_yunjin_variety_complexity ON yunjin_variety(complexity_score);

-- 云锦三大名锦种子数据
INSERT INTO yunjin_variety (code, name, alias, description, dynasty, weave_type,
    warp_count, weft_count, warp_density_per_cm, weft_density_per_cm,
    color_count, pattern_repeat_warp, pattern_repeat_weft,
    shedding_mechanism, harness_count, production_speed_cm_per_day,
    raw_material, silk_weight_per_sqm_gram,
    characteristics, historical_usage, cultural_significance,
    complexity_score, rarity_score, representative_works)
VALUES
('ZHUANGHUA', '妆花', '南京妆花缎',
 '云锦中工艺最复杂的品种，采用通经断纬工艺，色彩丰富，花纹立体饱满，是南京云锦的代表性品种。',
 '明代', '妆花缎',
 1400, 4, 56.0, 24.0,
 24, 240, 180,
 '大花楼木织机', 120, 4.5,
 '桑蚕丝+金线+孔雀羽', 780.0,
 '通经断纬，挖花盘织，逐花异色，花纹立体感强，色彩过渡自然',
 '明清宫廷御用龙袍、霞帔、诰命夫人服饰',
 '中国古代丝织工艺的巅峰之作，代表了云锦织造的最高水平',
 95, 9.5, '明万历帝龙袍、清乾隆朝袍'),

('KUJIN', '库锦', '织金库锦',
 '以金线为主要装饰的云锦品种，金光灿烂，富丽堂皇，因清代织入内务府广储司缎库而得名。',
 '清代', '织金缎',
 1200, 2, 60.0, 28.0,
 12, 180, 120,
 '大花楼木织机', 80, 10.0,
 '桑蚕丝+圆金线+扁金线', 720.0,
 '金线显花，花纹精致，色彩对比强烈，具有强烈的视觉冲击力',
 '宫廷服饰、官服补子、祭祀礼服、赏赐用锦',
 '体现了中国宫廷服饰的华贵气质，是等级制度的物质载体',
 78, 8.2, '清康熙织金云龙纹缎'),

('KUDUAN', '库缎', '本色库缎',
 '云锦中最基础的实用品种，以暗花为主要特征，质地紧密，光泽柔和，因入内务府缎库而得名。',
 '清代', '暗花缎',
 1000, 1, 58.0, 32.0,
 2, 120, 80,
 '小花楼木织机', 16, 25.0,
 '桑蚕丝', 420.0,
 '一上一下或一上三下组织，暗花隐现，质地细腻，手感滑爽',
 '日常服饰、袍料、被面、装饰用绸',
 '最具实用性的云锦品种，反映了清代丝绸生产的规模化水平',
 45, 5.0, '清雍正暗花缠枝莲缎'),

('LONG_LI', '云锦·龙纹', '龙纹锦',
 '以龙纹为主题的云锦品种，是皇权象征，工艺精湛，气势恢宏。',
 '明清', '妆花缎',
 1500, 5, 62.0, 22.0,
 18, 300, 200,
 '大花楼木织机', 150, 3.0,
 '桑蚕丝+金线+孔雀羽线', 850.0,
 '龙纹威严，云纹飘逸，海水江崖，布局严谨，色彩庄重',
 '皇帝龙袍、龙椅披、宫廷陈设',
 '中国龙文化的最高物质体现，皇权象征的集大成者',
 98, 9.8, '明万历黄缂丝十二章衮服'),

('LOTUS', '云锦·莲花纹', '缠枝莲锦',
 '以莲花为主题纹样的传统云锦，寓意纯洁吉祥，是民间最受欢迎的纹样之一。',
 '清代', '妆花缎',
 1200, 3, 58.0, 26.0,
 16, 160, 120,
 '大花楼木织机', 96, 6.0,
 '桑蚕丝+金线', 680.0,
 '缠枝莲造型优美，花叶婉转，色彩清雅，寓意吉祥',
 '民间婚嫁服饰、被面、装饰幔帐',
 '莲文化在丝织艺术中的完美体现，具有深厚的民俗意义',
 72, 6.5, '清乾隆缠枝莲纹妆花缎')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 纹样设计表 (pattern_design)
-- ============================================================
CREATE TABLE IF NOT EXISTS pattern_design (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    alias VARCHAR(500),
    pattern_code VARCHAR(50),
    category VARCHAR(100),
    sub_category VARCHAR(100),
    dynasty VARCHAR(100),
    origin VARCHAR(100),
    description VARCHAR(2000),
    cultural_meaning VARCHAR(2000),
    weave_structure VARCHAR(100),
    warp_repeat INTEGER,
    weft_repeat INTEGER,
    color_count INTEGER,
    tags VARCHAR(500),
    pattern_matrix TEXT,
    color_palette TEXT,
    complexity_level INTEGER,
    symmetry_score INTEGER,
    symmetry_type VARCHAR(50),
    representative_works VARCHAR(500),
    image_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    variety_id BIGINT,
    creator VARCHAR(100),
    is_public BOOLEAN DEFAULT TRUE,
    use_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pattern_code ON pattern_design(pattern_code);
CREATE INDEX IF NOT EXISTS idx_pattern_category ON pattern_design(category);
CREATE INDEX IF NOT EXISTS idx_pattern_dynasty ON pattern_design(dynasty);
CREATE INDEX IF NOT EXISTS idx_pattern_weave_structure ON pattern_design(weave_structure);
CREATE INDEX IF NOT EXISTS idx_pattern_variety ON pattern_design(variety_id);
CREATE INDEX IF NOT EXISTS idx_pattern_public ON pattern_design(is_public) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_pattern_use_count ON pattern_design(use_count DESC) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_pattern_tags ON pattern_design USING gin(to_tsvector('simple', COALESCE(tags, '')));

-- 纹样库种子数据
INSERT INTO pattern_design (name, pattern_code, category, sub_category, dynasty, origin,
    description, cultural_meaning, weave_structure, warp_repeat, weft_repeat,
    color_count, tags, complexity_level, symmetry_score, symmetry_type, variety_id, creator)
VALUES
('云龙纹', 'PAT-CLOUD-DRAGON', '动物纹', '龙纹', '明代', '南京',
 '经典的云纹与龙纹组合，龙在云中穿梭，气势磅礴，是明代宫廷最具代表性的纹样。',
 '龙象征皇权、威严、吉祥，云纹寓意高升如意',
 '妆花缎', 240, 180, 18,
 '龙,云,宫廷,皇权,明代,经典',
 90, 85, '左右对称', 1, '云锦研究所'),

('缠枝莲纹', 'PAT-LOTUS-VINE', '植物纹', '花卉纹', '清代', '苏州',
 '莲花以缠枝形式连续展开，花叶婉转优美，是传统吉祥纹样的代表。',
 '莲花象征纯洁、吉祥、出淤泥而不染，缠枝寓意生生不息',
 '妆花缎', 120, 80, 12,
 '莲花,缠枝,花卉,吉祥,清代,素雅',
 60, 90, '上下左右对称', 5, '云锦研究所'),

('牡丹花纹', 'PAT-PEONY', '植物纹', '花卉纹', '清代', '南京',
 '以牡丹花为主题的大花纹样，花型饱满，色彩丰富，被誉为花中之王。',
 '牡丹象征富贵、繁荣、美好，是传统吉祥纹样的核心题材',
 '妆花缎', 160, 120, 16,
 '牡丹,花卉,富贵,吉祥,清代,大花',
 75, 75, '左右对称', 1, '云锦研究所'),

('八吉祥纹', 'PAT-EIGHT-AUSPICIOUS', '宗教纹', '吉祥纹', '清代', '西藏',
 '藏传佛教八吉祥图案：法轮、法螺、宝伞、白盖、莲花、宝瓶、金鱼、盘长。',
 '八种吉祥物各有寓意，合称八吉祥，象征吉祥如意、幸福美满',
 '织金缎', 96, 96, 10,
 '八吉祥,佛教,吉祥,藏传,法器,宗教',
 65, 95, '中心对称', 2, '云锦研究所'),

('百子图', 'PAT-HUNDRED-CHILDREN', '人物纹', '婴戏', '明代', '苏州',
 '描绘百名童子嬉戏玩耍的热闹场景，姿态各异，生动活泼。',
 '百子寓意多子多福、子孙满堂，是传统吉祥文化的重要题材',
 '妆花缎', 300, 200, 20,
 '百子,婴戏,人物,吉祥,明代,多子',
 95, 70, '散点布局', 1, '云锦研究所'),

('万字纹', 'PAT-WANZI', '几何纹', '万字', '唐代', '印度传入',
 '卍字纹，佛教吉祥标志，武则天定音为"万"，寓意吉祥万德之所集。',
 '万字象征吉祥、万福万寿、永恒，常作为边饰或地纹',
 '暗花缎', 24, 24, 2,
 '万字,几何,佛教,吉祥,唐代,地纹',
 30, 100, '四方连续', 3, '云锦研究所'),

('海水江崖纹', 'PAT-SEA-CLIFF', '自然纹', '山水', '明代', '南京',
 '山崖立于海水之中，周围环绕祥云的纹样，常作为龙袍下摆装饰。',
 '海水江崖寓意江山永固、万世升平，是皇权服饰的标准纹样',
 '妆花缎', 200, 100, 12,
 '海水,江崖,山水,吉祥,明代,宫廷',
 70, 80, '左右对称', 4, '云锦研究所'),

('回纹', 'PAT-HUIWEN', '几何纹', '回纹', '商代', '中原',
 '由折线组成的回旋纹样，形如"回"字，是中国最古老的装饰纹样之一。',
 '回纹象征连绵不断、吉利永长，有"富贵不断头"的说法',
 '暗花缎', 32, 16, 2,
 '回纹,几何,传统,边饰,商周,古老',
 25, 95, '二方连续', 3, '云锦研究所'),

('凤穿牡丹', 'PAT-PHOENIX-PEONY', '动物纹', '凤纹', '清代', '南京',
 '凤凰穿梭于牡丹花丛中的纹样，凤与牡丹的完美结合，富贵吉祥。',
 '凤为百鸟之王，牡丹为百花之王，组合寓意荣华富贵、吉祥美好',
 '妆花缎', 180, 140, 18,
 '凤凰,牡丹,吉祥,清代,富贵,花鸟',
 85, 80, '左右对称', 1, '云锦研究所'),

('十二章纹', 'PAT-TWELVE-ORBERS', '礼制纹', '章服', '周代', '中原',
 '古代帝王礼服上的十二种纹饰：日、月、星辰、山、龙、华虫、宗彝、藻、火、粉米、黼、黻。',
 '十二章纹是古代等级制度的最高体现，各有其象征意义，合称十二章',
 '妆花缎', 360, 200, 24,
 '十二章,礼制,帝王,龙袍,周代,等级',
 98, 90, '对称布局', 4, '云锦研究所')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 色卡表 (color_palette)
-- ============================================================
CREATE TABLE IF NOT EXISTS color_palette (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    palette_type VARCHAR(50),
    source VARCHAR(100),
    dynasty VARCHAR(100),
    description VARCHAR(500),
    colors TEXT,
    color_count INTEGER,
    gamut_volume DOUBLE PRECISION,
    color_space VARCHAR(50),
    variety_id BIGINT,
    creator VARCHAR(100),
    is_public BOOLEAN DEFAULT TRUE,
    reference_image_url TEXT,
    cultural_notes VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_color_palette_code ON color_palette(code);
CREATE INDEX IF NOT EXISTS idx_color_palette_type ON color_palette(palette_type);
CREATE INDEX IF NOT EXISTS idx_color_palette_dynasty ON color_palette(dynasty);
CREATE INDEX IF NOT EXISTS idx_color_palette_variety ON color_palette(variety_id);

-- 传统色卡种子数据
INSERT INTO color_palette (name, code, palette_type, source, dynasty, description,
    colors, color_count, color_space, cultural_notes)
VALUES
('明代宫廷色卡', 'PAL-MING-COURT', 'traditional', '明定陵出土文物', '明代',
 '明代宫廷御用色卡，以朱红、石青、明黄为主色调，体现皇家威严。',
'朱砂红,#C82423,传统,矿物颜料,最高贵的红色
石青,#1A4B8C,冷色,矿物颜料,帝王冕服色
明黄,#FFD700,暖色,矿物颜料,皇帝专用
赭石色,#8B4513,暖色,矿物颜料,常用底色
艾绿,#7BA05B,冷色,植物染料,青绿色系
茄花紫,#5D3B6B,冷色,植物染料,高贵紫色
雪白,#FFFAF0,浅色,精练丝,本白底色
墨色,#2B2B2B,深色,植物染料,深黑近墨
绯红,#C41E3A,暖色,植物染料,鲜艳红色
鹦哥绿,#00AA88,冷色,植物染料,鲜亮绿色',
 10, 'RGB',
 '明代宫廷色彩制度严格，色彩是等级的标志。黄色为皇帝专用，文武官员按品级着不同颜色。'),

('清代妆花色卡', 'PAL-ZHUANGHUA', 'traditional', '清内务府织造局', '清代',
 '清代妆花缎专用色卡，色彩丰富艳丽，是云锦色彩的最高水平代表。',
'大红,#DC143C,主色,植物+矿物,喜庆主色
水红,#F8B4B4,辅助,植物染料,浅红过渡
粉红,#FFB6C1,辅助,植物染料,柔和粉色
桃红,#FF69B4,点缀,植物染料,鲜艳桃色
石青,#1B4F91,主色,矿物颜料,庄重蓝色
宝蓝,#4169E1,辅助,矿物颜料,明亮蓝色
月白,#F0F8FF,辅助,植物染料,极浅蓝
藏青,#213053,主色,矿物颜料,深沉藏青
明黄,#FFD700,主色,矿物颜料,帝王黄
鹅黄,#FFF68F,辅助,植物染料,浅嫩黄色
杏黄,#FFC87C,点缀,植物染料,橙黄
翠绿,#2E8B57,主色,植物染料,纯正绿色
豆绿,#BCEE68,辅助,植物染料,嫩绿
墨绿,#004225,辅助,植物染料,深沉绿色
茄紫,#483D8B,点缀,植物染料,深紫色
玫瑰紫,#BA55D3,点缀,植物染料,艳丽紫
金色,#FFD700,点缀,金线加工,富贵金
银色,#C0C0C0,点缀,银线加工,典雅银
玄色,#121212,主色,植物染料,黑色尊贵
象牙白,#FFFFF0,底色,精练丝,柔和白',
 20, 'RGB',
 '清代妆花用色可达二三十种，通过通经断纬工艺实现逐花异色，色彩过渡自然，被誉为"东方艺术明珠"。'),

('南京传统色名考', 'PAL-NANJING-NAMES', 'traditional', '《天工开物》《碎金》', '明代',
 '南京云锦传统色彩命名系统，以物喻色，形象生动，文化底蕴深厚。',
'天青,#87CEEB,雨后晴空,天青色等烟雨
虾青,#8F9E9E,如虾壳色,偏灰的青色
蟹壳青,#5A6C6C,蟹壳之色,深沉灰青
蛋青,#98D8EA,蛋清之色,浅淡青色
竹青,#789262,新竹之色,黄绿偏青
柳黄,#D4A017,嫩柳之色,黄绿色调
杏红,#FF743C,杏熟之色,红中带橙
石榴红,#9E1616,石榴花色,深红
朱砂,#FF461F,朱砂矿石,朱红
胭脂,#9E2F50,胭脂虫红,玫红
螺子黛,#302048,西域螺黛,深紫描眉
铜绿,#2A6049,铜锈之色,暗绿
藏花红,#C83E3A,藏传红花,深艳红
沉香色,#7A5C3E,沉香木色,棕黄
琥珀色,#FFBF00,琥珀之色,透明黄
秋香色,#B0A030,秋草之色,黄褐
驼色,#A67C52,驼毛之色,暖棕
猩血,#B8162A,猩猩之血,极深红',
 18, 'RGB',
 '中国传统色彩命名多取自自然万物，以物喻色，既有准确的色彩指向，又充满诗意和文化内涵。'),

('现代数码印花色卡', 'PAL-DIGITAL-PRINT', 'digital_printing', '现代数码印花工艺', '现代',
 '现代数码印花CMYK色卡，可实现1677万色全彩色，色彩精度高，渐变自然。',
'数码青,#00BFFF,CMYK青,青色原色
数码洋红,#FF00FF,CMYK洋红,品红原色
数码黄,#FFFF00,CMYK黄,黄色原色
数码黑,#000000,CMYK黑,黑色
喷绘红,#E60012,专用墨,喷绘机红
喷绘蓝,#005BAC,专用墨,喷绘机蓝
喷绘绿,#009944,专用墨,喷绘机绿
热转印橙,#FF6600,热升华,橙色
热转印紫,#9932CC,热升华,紫色
活性黑,#1A1A1A,活性染料,纯棉专用
酸性蓝,#0066CC,酸性染料,真丝专用
分散红,#E3007E,分散染料,涤纶专用
荧光黄,#E6FF00,荧光墨,高可见度
荧光红,#FF1493,荧光墨,高可见度
光变蓝,#4169E1,光变墨,光致变色
温变红,#FF4500,温变墨,热致变色
夜光绿,#98FB98,夜光墨,蓄光发光
珠光银,#C0C0C0,珠光墨,珍珠光泽
金属金,#FFD700,金属墨,金属质感
哑光黑,#202020,哑光墨,低光泽',
 20, 'CMYK/RGB',
 '现代数码印花采用CMYK四色叠印或多色专色，理论上可实现1677万种颜色，色域宽广，色彩过渡自然，适合复杂图案的大批量快速生产。');

-- ============================================================
-- 用户织造设计表 (user_weaving_design)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_weaving_design (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200),
    description VARCHAR(500),
    designer VARCHAR(100),
    base_variety_id BIGINT,
    base_variety_name VARCHAR(100),
    warp_count INTEGER,
    weft_count INTEGER,
    color_count INTEGER,
    pattern_matrix TEXT,
    color_palette TEXT,
    warp_tension_array TEXT,
    shed_opening_array TEXT,
    design_layers TEXT,
    thumbnail_url VARCHAR(500),
    pattern_id BIGINT,
    is_public BOOLEAN DEFAULT FALSE,
    like_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    complexity_score DOUBLE PRECISION,
    estimated_production_hours INTEGER,
    tags VARCHAR(1000),
    notes TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_design_designer ON user_weaving_design(designer);
CREATE INDEX IF NOT EXISTS idx_user_design_variety ON user_weaving_design(base_variety_id);
CREATE INDEX IF NOT EXISTS idx_user_design_public ON user_weaving_design(is_public) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_design_created ON user_weaving_design(created_at DESC) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_design_likes ON user_weaving_design(like_count DESC) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_design_status ON user_weaving_design(status);
CREATE INDEX IF NOT EXISTS idx_user_design_pattern ON user_weaving_design(pattern_id);
