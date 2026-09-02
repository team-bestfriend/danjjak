package com.bestfriend.danjjak.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactRequest;
import com.bestfriend.danjjak.support.mapper.SupportMapper;
import com.bestfriend.danjjak.support.model.GuardianContactRecord;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SupportServiceTest {

    private final SupportMapper supportMapper = mock(SupportMapper.class);
    private final SupportService supportService =
            new SupportService(
                    supportMapper,
                    new MockEnvironment()
                            .withProperty("support.customer-center-phone", "1588-0000"));

    @Test
    void returnsGuardianAndCustomerCenterPhone() {
        when(supportMapper.findGuardian(1L)).thenReturn(guardian(10L, "010-1234-5678"));

        var response = supportService.getSupport(1L);

        assertEquals(10L, response.guardian().guardianContactId());
        assertEquals("010-1234-5678", response.guardian().phoneNumber());
        assertEquals("1588-0000", response.customerCenterPhone());
    }

    @Test
    void updatesSingleGuardianContactAfterTrimming() {
        when(supportMapper.findGuardian(1L)).thenReturn(guardian(10L, "010-9999-0000"));

        var response =
                supportService.updateGuardian(
                        1L, new GuardianContactRequest(" 010-9999-0000 "));

        verify(supportMapper).upsertGuardian(1L, "010-9999-0000");
        assertEquals("010-9999-0000", response.phoneNumber());
    }

    private GuardianContactRecord guardian(long id, String phoneNumber) {
        GuardianContactRecord record = new GuardianContactRecord();
        record.setGuardianContactId(id);
        record.setPhoneNumber(phoneNumber);
        return record;
    }
}
