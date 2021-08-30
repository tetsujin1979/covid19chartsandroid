package com.covid19iecharts;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface Covid19ChartsInterface {

    @LambdaFunction(functionName = "Covid19IECharts")
    LambdaOutput Covid19IECharts(LambdaInput input);

}
