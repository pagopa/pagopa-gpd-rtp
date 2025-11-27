package it.gov.pagopa.gpd.rtp.repository;

import it.gov.pagopa.gpd.rtp.entity.PaymentPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtPositionRepository
    extends JpaRepository<PaymentPosition, Long>, JpaSpecificationExecutor<PaymentPosition> {}
