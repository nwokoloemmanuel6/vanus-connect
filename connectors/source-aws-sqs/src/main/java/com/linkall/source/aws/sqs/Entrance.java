package com.linkall.source.aws.sqs;

import com.linkall.vance.core.VanceApplication;

public class Entrance {
    public static void main(String[] args) {
        VanceApplication.run(SqsSource.class);
    }
}
