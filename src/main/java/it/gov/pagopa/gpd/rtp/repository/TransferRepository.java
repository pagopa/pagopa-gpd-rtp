package it.gov.pagopa.gpd.rtp.repository;

import it.gov.pagopa.gpd.rtp.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByPaymentOptionId(long paymentOptionId);
}
