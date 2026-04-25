package com.example.recruitment;

import com.example.recruitment.common.Result;
import com.example.recruitment.common.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 通用返回对象单元测试
 */
@DisplayName("Result 统一响应类测试")
class ResultTest {

    @Test
    @DisplayName("success(data) - 成功带数据")
    void testSuccessWithData() {
        String data = "test data";
        Result<String> result = Result.success(data);

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMsg(), result.getMsg());
        assertEquals("test data", result.getData());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("success() - 成功无数据")
    void testSuccessWithoutData() {
        Result<Void> result = Result.success();

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNull(result.getData(), "无参数的success方法data应为null");
    }

    @Test
    @DisplayName("failed(ResultCode, msg) - 失败带错误码")
    void testFailedWithCode() {
        Result<Object> result = Result.failed(ResultCode.UNAUTHORIZED, "未登录");

        assertEquals(401, result.getCode());
        assertEquals("未登录", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("failed(msg) - 失败默认使用FAILED码")
    void testFailedWithString() {
        Result<Object> result = Result.failed("操作失败");

        assertEquals(ResultCode.FAILED.getCode(), result.getCode());
        assertEquals("操作失败", result.getMsg());
    }

    @Test
    @DisplayName("validateFailed(msg) - 校验失败")
    void testValidateFailed() {
        Result<Object> result = Result.validateFailed("用户名不能为空");

        assertEquals(422, result.getCode());
        assertEquals("用户名不能为空", result.getMsg());
    }

    @Test
    @DisplayName("Setter/Getter 正确性")
    void testSettersAndGetters() {
        Result<String> result = new Result<>();

        result.setCode(200);
        result.setMsg("ok");
        result.setData("payload");

        assertEquals(200, result.getCode());
        assertEquals("ok", result.getMsg());
        assertEquals("payload", result.getData());
    }
}
