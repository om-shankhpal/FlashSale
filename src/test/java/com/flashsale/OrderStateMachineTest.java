
package com.flashsale;

import com.flashsale.entity.OrderStatus;
import com.flashsale.service.OrderStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    private final OrderStateMachine stateMachine =
            new OrderStateMachine();

    @Test
    void shouldAllowPendingPaymentToPaid() {
        assertTrue(stateMachine.canTransition(
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID
        ));
    }

    @Test
    void shouldAllowPaidToConfirmed() {
        assertTrue(stateMachine.canTransition(
                OrderStatus.PAID,
                OrderStatus.CONFIRMED
        ));
    }

    @Test
    void shouldRejectPendingPaymentToConfirmed() {
        assertFalse(stateMachine.canTransition(
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.CONFIRMED
        ));
    }

    @Test
    void shouldRejectTransitionFromConfirmedToCancelled() {
        assertFalse(stateMachine.canTransition(
                OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED
        ));
    }

    @Test
    void shouldThrowExceptionForInvalidTransition() {
        assertThrows(
                IllegalStateException.class,
                () -> stateMachine.validateTransition(
                        OrderStatus.PENDING_PAYMENT,
                        OrderStatus.CONFIRMED
                )
        );
    }
}