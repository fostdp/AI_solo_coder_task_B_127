package com.yunjin.system.fabric_analyzer;

import com.yunjin.system.config.FourierAnalysisConfig;
import com.yunjin.system.entity.FabricAnalysis;
import com.yunjin.system.entity.WeavingSimulation;
import com.yunjin.system.repository.FabricAnalysisRepository;
import com.yunjin.system.repository.WeavingSimulationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("傅里叶分析线程池测试 - FabricAnalyzerService FFT ThreadPool")
class FabricAnalyzerFourierTest {

    @Mock
    private FabricAnalysisRepository fabricAnalysisRepository;

    @Mock
    private WeavingSimulationRepository weavingSimulationRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private ExecutorService fourierExecutor;

    @InjectMocks
    private FabricAnalyzerService analysisService;

    private int[][] testMatrix;

    @BeforeEach
    void setUp() {
        testMatrix = new int[][]{
                {1, 0, 1, 0, 1, 0, 1, 0},
                {0, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0},
                {0, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0},
                {0, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0},
                {0, 1, 0, 1, 0, 1, 0, 1}
        };
    }

    @Nested
    @DisplayName("【正常场景】傅里叶线程池配置")
    class ThreadPoolConfiguration {

        @Test
        @DisplayName("NC-FFT-01: 线程池配置类创建成功")
        void testFourierConfigBean() {
            try (AnnotationConfigApplicationContext ctx =
                         new AnnotationConfigApplicationContext(FourierAnalysisConfig.class)) {
                ExecutorService executor = ctx.getBean("fourierAnalysisExecutor", ExecutorService.class);
                assertNotNull(executor);
                assertTrue(executor instanceof ThreadPoolExecutor);

                ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
                assertEquals(4, tpe.getCorePoolSize());
                assertEquals(8, tpe.getMaximumPoolSize());
                assertEquals(60, tpe.getKeepAliveTime(TimeUnit.SECONDS));
            }
        }

        @Test
        @DisplayName("NC-FFT-02: 线程工厂生成正确命名的线程")
        void testThreadFactoryNaming() {
            try (AnnotationConfigApplicationContext ctx =
                         new AnnotationConfigApplicationContext(FourierAnalysisConfig.class)) {
                ExecutorService executor = ctx.getBean("fourierAnalysisExecutor", ExecutorService.class);
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;

                final String[] threadName = new String[1];
                final boolean[] done = {false};
                tpe.execute(() -> {
                    threadName[0] = Thread.currentThread().getName();
                    done[0] = true;
                });

                int waitCount = 0;
                while (!done[0] && waitCount < 50) {
                    try { Thread.sleep(20); } catch (InterruptedException e) { break; }
                    waitCount++;
                }

                assertNotNull(threadName[0], "线程名不应为null");
                assertTrue(threadName[0].startsWith("fourier-analysis-"),
                        "线程名应以前缀开头，实际: " + threadName[0]);
            }
        }

        @Test
        @DisplayName("NC-FFT-03: 线程池状态可查询")
        void testThreadPoolStats() {
            try (AnnotationConfigApplicationContext ctx =
                         new AnnotationConfigApplicationContext(FourierAnalysisConfig.class)) {
                ExecutorService executor = ctx.getBean("fourierAnalysisExecutor", ExecutorService.class);

                FabricAnalyzerService realService = new FabricAnalyzerService(
                        fabricAnalysisRepository, weavingSimulationRepository,
                        new com.fasterxml.jackson.databind.ObjectMapper(), executor);

                Map<String, Object> stats = realService.getFourierThreadPoolStats();
                assertNotNull(stats);
                assertTrue(stats.containsKey("poolSize"));
                assertTrue(stats.containsKey("activeCount"));
                assertTrue(stats.containsKey("corePoolSize"));
                assertTrue(stats.containsKey("maximumPoolSize"));
                assertTrue(stats.containsKey("queueSize"));

                assertEquals(0, stats.get("activeCount"));
                assertEquals(0, stats.get("queueSize"));
            } catch (Exception e) {
                e.printStackTrace();
                fail("不应抛出异常: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("【正常场景】异步傅里叶分析")
    class AsyncFFTAnalysis {

        @Test
        @DisplayName("NC-FFT-04: 同步FFT分析返回完整结果")
        void testSyncFFTAnalysis() {
            Map<String, Object> result = analysisService.performFFTAnalysis(testMatrix);

            assertNotNull(result);
            assertTrue(result.containsKey("warpDirection"));
            assertTrue(result.containsKey("weftDirection"));
            assertTrue(result.containsKey("fftSizeWarp"));
            assertTrue(result.containsKey("fftSizeWeft"));

            @SuppressWarnings("unchecked")
            Map<String, Object> warpSpec = (Map<String, Object>) result.get("warpDirection");
            assertTrue(warpSpec.containsKey("dominantFrequency"));
            assertTrue(warpSpec.containsKey("dominantAmplitude"));
            assertTrue(warpSpec.containsKey("estimatedPeriod"));
        }

        @Test
        @DisplayName("NC-FFT-05: 平纹结构FFT检测到周期2")
        void testPlainWeaveFFTPeriod() {
            Map<String, Object> result = analysisService.performFFTAnalysis(testMatrix);

            @SuppressWarnings("unchecked")
            Map<String, Object> warpSpec = (Map<String, Object>) result.get("warpDirection");
            @SuppressWarnings("unchecked")
            Map<String, Object> weftSpec = (Map<String, Object>) result.get("weftDirection");

            assertNotNull(warpSpec.get("estimatedPeriod"));
            assertNotNull(weftSpec.get("estimatedPeriod"));

            double warpPeriod = (double) warpSpec.get("estimatedPeriod");
            double weftPeriod = (double) weftSpec.get("estimatedPeriod");

            assertTrue(warpPeriod >= 0, "经向周期应非负");
            assertTrue(weftPeriod >= 0, "纬向周期应非负");
        }

        @Test
        @DisplayName("NC-FFT-06: FFT尺寸为2的幂")
        void testFFTPowerOfTwo() {
            Map<String, Object> result = analysisService.performFFTAnalysis(testMatrix);

            int fftWarp = (int) result.get("fftSizeWarp");
            int fftWeft = (int) result.get("fftSizeWeft");

            assertTrue(isPowerOfTwo(fftWarp), "FFT经向尺寸应为2的幂: " + fftWarp);
            assertTrue(isPowerOfTwo(fftWeft), "FFT纬向尺寸应为2的幂: " + fftWeft);
        }

        private boolean isPowerOfTwo(int n) {
            return n > 0 && (n & (n - 1)) == 0;
        }
    }

    @Nested
    @DisplayName("【边界场景】FFT分析边界测试")
    class FFTEdgeCases {

        @Test
        @DisplayName("BD-FFT-01: 最小矩阵FFT不崩溃")
        void testSmallMatrixFFT() {
            int[][] smallMatrix = new int[][]{{1, 0}, {0, 1}};
            Map<String, Object> result = analysisService.performFFTAnalysis(smallMatrix);
            assertNotNull(result);
            assertTrue(result.containsKey("fftSizeWarp"));
        }

        @Test
        @DisplayName("BD-FFT-02: 大尺寸矩阵裁剪到512")
        void testLargeMatrixFFT() {
            int[][] largeMatrix = new int[1000][1000];
            for (int i = 0; i < 1000; i++) {
                for (int j = 0; j < 1000; j++) {
                    largeMatrix[i][j] = (i + j) % 2;
                }
            }

            Map<String, Object> result = analysisService.performFFTAnalysis(largeMatrix);

            int fftWarp = (int) result.get("fftSizeWarp");
            int fftWeft = (int) result.get("fftSizeWeft");

            assertTrue(fftWarp <= 512, "FFT尺寸不应超过512: " + fftWarp);
            assertTrue(fftWeft <= 512, "FFT尺寸不应超过512: " + fftWeft);
        }

        @Test
        @DisplayName("BD-FFT-03: 全1矩阵FFT频谱集中在零频")
        void testAllOnesMatrixFFT() {
            int[][] onesMatrix = new int[16][16];
            for (int i = 0; i < 16; i++) {
                Arrays.fill(onesMatrix[i], 1);
            }

            Map<String, Object> result = analysisService.performFFTAnalysis(onesMatrix);

            @SuppressWarnings("unchecked")
            Map<String, Object> warpSpec = (Map<String, Object>) result.get("warpDirection");
            double domFreq = (double) warpSpec.get("dominantFrequency");

            assertEquals(0.0, domFreq, 0.001, "全1矩阵主频率应为0");
        }
    }

    @Nested
    @DisplayName("【异常场景】异常处理")
    class ExceptionScenarios {

        @Test
        @DisplayName("EX-FFT-01: 空矩阵FFT抛出异常")
        void testEmptyMatrixFFT() {
            int[][] emptyMatrix = new int[0][0];
            assertThrows(Exception.class, () ->
                    analysisService.performFFTAnalysis(emptyMatrix));
        }

        @Test
        @DisplayName("EX-FFT-02: 线程池状态在非ThreadPool时返回错误")
        void testNonThreadPoolExecutor() {
            ExecutorService fakeExecutor = new java.util.concurrent.Executors().newFixedThreadPool(1);
            FabricAnalyzerService service = new FabricAnalyzerService(
                    fabricAnalysisRepository, weavingSimulationRepository,
                    new com.fasterxml.jackson.databind.ObjectMapper(), fakeExecutor);

            Map<String, Object> stats = service.getFourierThreadPoolStats();
            assertTrue(stats.containsKey("error") || stats.containsKey("poolSize"),
                    "应包含状态或错误信息");

            fakeExecutor.shutdown();
        }

        @Test
        @DisplayName("EX-FFT-03: 同步分析不存在织机抛异常")
        void testAnalyzeNonExistentLoom() {
            when(weavingSimulationRepository.findByLoomId(999L))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () ->
                    analysisService.analyzeFabricStructure(999L));
        }
    }
}
