package com.bestfriend.danjjak.support.service;

import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactRequest;
import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactResponse;
import com.bestfriend.danjjak.support.dto.SupportDtos.SupportResponse;
import com.bestfriend.danjjak.support.mapper.SupportMapper;
import com.bestfriend.danjjak.support.model.GuardianContactRecord;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportService {

    private final SupportMapper supportMapper;
    private final String customerCenterPhone;

    public SupportService(SupportMapper supportMapper, Environment environment) {
        this.supportMapper = supportMapper;
        this.customerCenterPhone =
                environment.getRequiredProperty("support.customer-center-phone");
    }

    @Transactional(readOnly = true)
    public SupportResponse getSupport(long userId) {
        return new SupportResponse(
                toResponse(supportMapper.findGuardian(userId)), customerCenterPhone);
    }

    @Transactional
    public GuardianContactResponse updateGuardian(
            long userId, GuardianContactRequest request) {
        supportMapper.upsertGuardian(userId, request.phoneNumber().trim());
        return toResponse(supportMapper.findGuardian(userId));
    }

    private GuardianContactResponse toResponse(GuardianContactRecord record) {
        return record == null
                ? null
                : new GuardianContactResponse(
                        record.getGuardianContactId(), record.getPhoneNumber());
    }
}
