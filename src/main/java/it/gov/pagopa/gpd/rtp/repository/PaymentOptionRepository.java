package it.gov.pagopa.gpd.rtp.repository;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOptionRepository
    extends JpaRepository<PaymentOption, Long>, JpaSpecificationExecutor<PaymentOption> { }
