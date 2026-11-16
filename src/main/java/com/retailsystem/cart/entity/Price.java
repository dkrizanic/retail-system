package com.retailsystem.cart.entity;

import java.math.BigDecimal;

public sealed interface Price permits OneTimePrice, RecurringPrice {

    BigDecimal value();
}
