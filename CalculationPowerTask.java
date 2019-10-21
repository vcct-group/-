package group.vcct.common.mq.task;

import group.vcct.common.mq.bean.VcctSystemConfigBean;
import group.vcct.common.mq.service.CalculationPowerService;
import group.vcct.common.mq.utils.Common;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculationPowerTask implements Job {
    private static Logger logger = LoggerFactory.getLogger(CalculationPowerTask.class);
    @Autowired
    private CalculationPowerService calculationPowerService;
    @Value("${vcct.system.key}")
    private String dataKey;

    /**
     * 每天统计算力价格
     * 最新算力价格 = （前天全网挖矿总算力 / 昨日挖矿总算力）* 当前算力价格
     *
     * @param context
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("每天统计算力价格--ING");
        //查询当前算力价格
        VcctSystemConfigBean calculationPowerBean = calculationPowerService.getCalculationPowerRatio(dataKey);
        if (null == calculationPowerBean) {
            //写入配置
            try {
                calculationPowerService.insert();
            }catch (Exception e) {
                logger.error("写入失败，原因：" + e.getMessage());
                return;
            }
        } else {
            Double calculationPower = Double.valueOf(calculationPowerBean.getsValue());
            //获取前天日期
            Long date1 = Common.getBeforeDay(2);
            //获取昨日日期
            Long date2 = Common.getBeforeDay(1);
            //获取前天挖矿总算力
            BigDecimal diggingCalculationPower1 = calculationPowerService.getDiggingCalculationPower(date1);
            //获取昨日挖矿总算力
            BigDecimal diggingCalculationPower2 = calculationPowerService.getDiggingCalculationPower(date2);
            //设置算力调整比率
            BigDecimal ratio = diggingCalculationPower1.divide(diggingCalculationPower2,10, RoundingMode.FLOOR);
            //最终算力比率
            String determineCalculationPower = (ratio.multiply(BigDecimal.valueOf(calculationPower))).setScale(5, RoundingMode.FLOOR).toString();
            logger.info("最终算力比率determineCalculationPower: " + determineCalculationPower);
            //修改配置
            try {
                calculationPowerService.setCalculationPowerRatio(determineCalculationPower, dataKey);
            }catch (Exception e) {
                logger.error("修改失败，原因：" + e.getMessage());
                return;
            }
        }

        logger.info("每天统计算力价格--OK");
    }
}
