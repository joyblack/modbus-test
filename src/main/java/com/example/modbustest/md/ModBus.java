package com.example.modbustest.md;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadCoilsRequest;
import com.digitalpetri.modbus.requests.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadCoilsResponse;
import com.digitalpetri.modbus.responses.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Desc:
 * ModBus组件
 *
 * @author JoyBlack
 * @email 1016037677@qq.com
 * @date 2023/1/6 下午 3:27
 */
public class ModBus {
    private ModbusTcpMaster master;

    private static final Logger logger = LoggerFactory.getLogger(ModBus.class);

    public ModBus(String address, int port) {
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address)
                .setPort(port)
                .setTimeout(Duration.ofSeconds(20))
                .build();
        master = new ModbusTcpMaster(config);
    }

    /**
     * Desc:
     * 释放资源
     *
     * @author JoyBlack
     * @email 1016037677@qq.com
     * @date 2023/1/5 上午 10:04
     */
    public void release() {
        if (master != null) {
            master.disconnect();
        }
        // Modbus.releaseSharedResources();
    }

    /**
     * Desc:
     * 读取HoldingRegister数据(address: 寄存器地址; quantity: 寄存器数量; unitId: id)
     *
     * @author JoyBlack
     * @email 1016037677@qq.com
     * @date 2023/1/5 上午 10:04
     */
    public Number readHoldingRegister(int address, int quantity, int unitId) throws Exception {
        Number result = null;
        CompletableFuture<ReadHoldingRegistersResponse> future = master.sendRequest(new ReadHoldingRegistersRequest(address, quantity), unitId);
        ReadHoldingRegistersResponse response = future.get(8, TimeUnit.SECONDS);
        if (response != null) {
            ByteBuf registers = response.getRegisters();
            result = registers.readFloat();
            ReferenceCountUtil.release(response);
        }
        return result;
    }

    /**
     * Desc:
     * 读取InputRegisters模拟量数据(address: 寄存器地址; quantity: 寄存器数量; unitId: id)
     *
     * @author JoyBlack
     * @email 1016037677@qq.com
     * @date 2023/1/5 上午 10:04
     */
    public Number readInputRegister(int address, int quantity, int unitId) throws Exception {
        Number result = null;
        CompletableFuture<ReadInputRegistersResponse> future = master
                .sendRequest(new ReadInputRegistersRequest(address, quantity), unitId);
        // 工具类做的同步返回.实际使用推荐结合业务进行异步处理
        ReadInputRegistersResponse readInputRegistersResponse = future.get();
        if (readInputRegistersResponse != null) {
            ByteBuf buf = readInputRegistersResponse.getRegisters();
            result = buf.readFloat();
            ReferenceCountUtil.release(readInputRegistersResponse);
        }
        return result;
    }

    /**
     * Desc:
     * 读取Coils开关量
     *
     * @author JoyBlack
     * @email 1016037677@qq.com
     * @date 2023/1/5 上午 10:04
     */
    public Boolean readCoils(int address, int quantity, int unitId)
            throws Exception {
        Boolean result = null;
        CompletableFuture<ReadCoilsResponse> future = master.sendRequest(new ReadCoilsRequest(address, quantity),
                unitId);
        ReadCoilsResponse readCoilsResponse = future.get();
        if (readCoilsResponse != null) {
            ByteBuf buf = readCoilsResponse.getCoilStatus();
            result = buf.readBoolean();
            ReferenceCountUtil.release(readCoilsResponse);
        }
        return result;
    }

    /**
     * Desc:
     * 读取readDiscreteInputs开关量
     *
     * @author JoyBlack
     * @email 1016037677@qq.com
     * @date 2023/1/5 上午 10:04
     */
    public Boolean readDiscreteInputs(int address, int quantity, int unitId)
            throws Exception {
        Boolean result = null;
        CompletableFuture<ReadDiscreteInputsResponse> future = master
                .sendRequest(new ReadDiscreteInputsRequest(address, quantity), unitId);
        ReadDiscreteInputsResponse discreteInputsResponse = future.get();
        if (discreteInputsResponse != null) {
            ByteBuf buf = discreteInputsResponse.getInputStatus();
            result = buf.readBoolean();
            ReferenceCountUtil.release(discreteInputsResponse);
        }
        return result;
    }

    public Result<Map<Integer, Number>> run(int start, int end, int quantity, int unitId) {
        Map<Integer, Number> result = new HashMap<>();
        try {
            for (int i = start; i <= end; i += 2) {
                Number value = readHoldingRegister(i, quantity, unitId);
                if ("infinity".equals(value.toString().toLowerCase())) {
                    logger.error(String.format("点位[%s]数据转换失败,设置为默认值 null", i));
                    result.put(i, null);
                } else {
                    result.put(i, value);
                }
            }
            return new Result<Map<Integer, Number>>().ok(result);
        } catch (Exception e) {
            String msg = String.format("【监控数据采集任务】获取监控数据失败：%s, start = %d, end = %d.", e.getMessage(), start, end);
            logger.error(msg);
            Result<Map<Integer, Number>> error = new Result<Map<Integer, Number>>().error(msg);
            return error;
        }
    }

    public static void main(String[] args) {
        try {
            ModBus modBus = new ModBus("127.0.0.1", 502);
            for (int i = 0; i < 10; i++) {
                Result<Map<Integer, Number>> run = modBus.run(0, 10, 2, 1);
                System.out.println(run.getData());
                Thread.sleep(1000);
            }
            // 释放资源
            modBus.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
