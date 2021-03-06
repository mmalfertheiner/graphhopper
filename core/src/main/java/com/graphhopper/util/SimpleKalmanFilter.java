package com.graphhopper.util;

import java.util.Arrays;

/**
 * Explanation of the Kalman filter
 *
 * http://bilgin.esme.org/BitsBytes/KalmanFilterforDummies.aspx
 */


public class SimpleKalmanFilter implements SmoothingFilter{

    public final static int FORWARD = 1;
    public final static int BACKWARD = 2;
    public final static int COMBINED = 3;

    private int direction = FORWARD;

    private double[] estimates; //Array with estimated values
    private double[] error; //Array with error covariance

    private double r = 6;   //Environment noise - default 6 for Europe
    private Double q = 0.1;

    private double[] processNoise;
    private int processNoiseScalingFactor;


    public SimpleKalmanFilter(){}


    public SimpleKalmanFilter(int direction, double eNoise, double processNoise) {

        this.direction = direction;
        this.r = eNoise;
        setStaticProcessNoise(processNoise);

    }

    public SimpleKalmanFilter(int direction, double eNoise, double[] processNoise, int processNoiseScalingFactor) {
        this.direction = direction;
        this.r = eNoise;
        setDynamicProcessNoise(processNoise, processNoiseScalingFactor);
    }

    public void setStaticProcessNoise(double processNoise){
        this.q = processNoise;
        this.processNoise = null;
    }

    public void setDynamicProcessNoise(double[] processNoise, int processNoiseScalingFactor){
        this.q = null;
        this.processNoise = processNoise;
        this.processNoiseScalingFactor = processNoiseScalingFactor;
    }

    private void init(double[] originalMeasurements){
        estimates = new double[originalMeasurements.length + 2];
        error = new double[originalMeasurements.length + 2];

        estimates[0] = originalMeasurements[0];
        error[0] = 1;

        estimates[estimates.length - 1] = originalMeasurements[originalMeasurements.length - 1];
        error[error.length - 1] = 1;
    }

    private double getQ(int index, int originalMeasurementsSize){
        if(q != null)
            return q;

        int missingProcessNoise = originalMeasurementsSize - processNoise.length;

        if(missingProcessNoise > 0 && index < missingProcessNoise)
            return 0;
        else if (missingProcessNoise > 0)
            return processNoise[index - missingProcessNoise] / processNoiseScalingFactor;

        return processNoise[index] / processNoiseScalingFactor;
    }


    private void kalman(final double[] originalMeasurements, int estimatesIndex, int followIndex, int originalIndex){
        // Time update
        double xPrior = 1 * estimates[estimatesIndex];
        double pPrior = error[estimatesIndex] + getQ(originalIndex, originalMeasurements.length);

        //System.out.println(getQ(originalIndex, originalMeasurements.length));

        // Measurement update
        double kalmanGain = pPrior / (pPrior + r);

        double estimate = xPrior + kalmanGain * (originalMeasurements[originalIndex] - xPrior);
        estimates[followIndex] = estimate;

        double error = (1 - kalmanGain) * pPrior;
        this.error[followIndex] = error;

        //System.out.println(Helper.round2(originalMeasurements[originalIndex]) + " & " + Helper.round2(xPrior) + " & " + Helper.round2(pPrior) + " & " + Helper.round2(kalmanGain) + " & " + Helper.round2(estimate) + " & " + Helper.round2(error) + " \\\\");
    }

    @Override
    public double[] smooth(final double[] measurements) {

        init(measurements);

        if(direction == FORWARD) {
            for (int i = 0; i < measurements.length; i++) {
                kalman(measurements, i, i + 1, i);
            }
        } else if (direction == BACKWARD) {
            for (int i = measurements.length - 1; i >= 0; i--) {
                kalman(measurements, i+2, i + 1, i);
            }
        } else if (direction == COMBINED) {

            direction = FORWARD;
            double[] tmpF = smooth(measurements);

            direction = BACKWARD;
            double[] tmpB = smooth(measurements);

            for (int i = 0; i < measurements.length; i++) {
                estimates[i+1] = (tmpF[i] + tmpB[i]) / 2;
            }
        }

        return getFilteredValues();
    }

    public double[] getFilteredValues(){

        if(estimates != null){
            return Arrays.copyOfRange(estimates, 1, estimates.length - 1);
        }

        return null;
    }



    public static void main(String[] args) {

        double data1[] = {0.390, 0.5, 0.48, 0.29, 0.25, 0.320, 0.34, 0.48, 0.41, 0.45};
        //double data2[] = {0.45, 0.41, 0.48, 0.34, 0.320, 0.25, 0.29, 0.48, 0.5, 0.390};
        //double data[] = {1009.0, 1009.0, 1009.0, 1009.0, 1009.0, 1009.0, 1012.0, 1017.0, 1017.0, 1024.0, 1024.0, 1023.0, 1022.0, 1026.0, 1028.0, 1029.0, 1031.0, 1031.0, 1033.0, 1038.0, 1041.0, 1041.0, 1041.0, 1041.0, 1043.0, 1048.0, 1056.0, 1061.0, 1061.0, 1067.0, 1071.0, 1071.0, 1070.0, 1070.0, 1069.0, 1072.0, 1075.0, 1080.0, 1076.0, 1076.0, 1076.0, 1076.0, 1076.0, 1081.0, 1085.0, 1085.0, 1085.0, 1088.0, 1088.0, 1088.0, 1088.0, 1091.0, 1091.0, 1091.0, 1090.0, 1090.0, 1090.0, 1090.0, 1090.0, 1090.0, 1090.0, 1091.0, 1092.0, 1092.0, 1092.0, 1095.0, 1098.0, 1098.0, 1098.0, 1098.0, 1098.0, 1098.0, 1098.0, 1100.0, 1102.0, 1103.0, 1104.0, 1108.0, 1109.0, 1109.0, 1111.0, 1114.0, 1116.0, 1119.0, 1119.0, 1119.0, 1127.0, 1132.0, 1132.0, 1132.0, 1132.0, 1139.0, 1150.0, 1150.0, 1150.0, 1154.0, 1154.0, 1154.0, 1158.0, 1158.0, 1158.0, 1158.0, 1158.0, 1158.0, 1158.0, 1160.0, 1159.0, 1160.0, 1162.0, 1162.0, 1162.0, 1162.0, 1162.0, 1159.0, 1159.0, 1159.0, 1159.0, 1159.0, 1166.0, 1166.0, 1166.0, 1166.0, 1175.0, 1175.0, 1175.0, 1175.0, 1175.0, 1175.0, 1172.0, 1172.0, 1182.0, 1181.0, 1188.0, 1193.0, 1192.0, 1192.0, 1192.0, 1198.0, 1195.0, 1195.0, 1200.0, 1197.0, 1202.0, 1199.0, 1206.0, 1203.0, 1204.0, 1204.0, 1204.0, 1204.0, 1209.0, 1209.0, 1209.0, 1209.0, 1209.0, 1206.0, 1206.0, 1206.0, 1211.0, 1213.0, 1210.0, 1206.0, 1208.0, 1206.0, 1210.0, 1210.0, 1214.0, 1220.0, 1226.0, 1230.0, 1230.0, 1228.0, 1228.0, 1229.0, 1229.0, 1234.0, 1234.0, 1232.0, 1238.0, 1237.0, 1243.0, 1248.0, 1245.0, 1250.0, 1254.0, 1254.0, 1258.0, 1266.0, 1264.0, 1270.0, 1266.0, 1273.0, 1280.0, 1292.0, 1292.0, 1292.0, 1292.0, 1292.0, 1292.0, 1292.0, 1297.0, 1303.0, 1303.0, 1299.0, 1308.0, 1306.0, 1306.0, 1309.0, 1311.0, 1317.0, 1317.0, 1319.0, 1330.0, 1330.0, 1330.0, 1330.0, 1330.0, 1330.0, 1325.0, 1319.0, 1330.0, 1330.0, 1330.0, 1330.0, 1330.0, 1330.0, 1322.0, 1322.0, 1322.0, 1335.0, 1333.0, 1343.0, 1341.0, 1340.0, 1340.0, 1343.0, 1348.0, 1346.0, 1355.0, 1355.0, 1356.0, 1363.0, 1362.0, 1362.0, 1365.0, 1365.0, 1368.0, 1368.0, 1372.0, 1372.0, 1376.0, 1376.0, 1390.0, 1383.0, 1383.0, 1383.0, 1383.0, 1383.0, 1380.0, 1380.0, 1380.0, 1380.0, 1379.0, 1379.0, 1379.0, 1379.0, 1379.0, 1379.0, 1379.0, 1379.0, 1379.0, 1384.0, 1382.0, 1382.0, 1382.0, 1382.0, 1374.0, 1394.0, 1399.0, 1387.0, 1388.0, 1378.0, 1393.0, 1391.0, 1391.0, 1388.0, 1388.0, 1393.0, 1398.0, 1408.0, 1408.0, 1408.0, 1408.0, 1408.0, 1408.0, 1408.0, 1422.0, 1400.0, 1412.0, 1412.0, 1426.0, 1400.0, 1415.0, 1428.0, 1429.0, 1429.0, 1439.0, 1439.0, 1439.0, 1425.0, 1425.0, 1425.0, 1434.0, 1434.0, 1432.0, 1432.0, 1432.0, 1428.0, 1428.0, 1438.0, 1428.0, 1424.0, 1436.0, 1445.0, 1443.0, 1451.0, 1455.0, 1451.0, 1451.0, 1443.0, 1443.0, 1452.0, 1468.0, 1475.0, 1464.0, 1472.0, 1465.0, 1465.0, 1465.0, 1465.0, 1475.0, 1469.0, 1476.0, 1478.0, 1478.0, 1479.0, 1489.0, 1489.0, 1488.0, 1489.0, 1489.0, 1498.0, 1498.0, 1498.0, 1503.0, 1502.0, 1501.0, 1490.0, 1498.0, 1502.0, 1510.0, 1527.0, 1510.0, 1524.0, 1513.0, 1513.0, 1519.0, 1501.0, 1506.0, 1506.0, 1506.0, 1501.0, 1504.0, 1511.0, 1530.0, 1537.0, 1537.0, 1537.0, 1540.0, 1540.0, 1540.0, 1540.0, 1540.0, 1540.0, 1539.0, 1539.0, 1539.0, 1539.0, 1539.0, 1539.0, 1539.0, 1539.0, 1525.0, 1524.0, 1524.0, 1535.0, 1558.0, 1564.0, 1552.0, 1552.0, 1558.0, 1565.0, 1560.0, 1560.0, 1560.0, 1560.0, 1560.0, 1567.0, 1567.0, 1567.0, 1572.0, 1557.0, 1559.0, 1559.0, 1559.0, 1563.0, 1563.0, 1563.0, 1580.0, 1578.0, 1578.0, 1578.0, 1578.0, 1578.0, 1587.0, 1586.0, 1580.0, 1587.0, 1581.0, 1585.0, 1579.0, 1592.0, 1589.0, 1589.0, 1589.0, 1589.0, 1575.0, 1597.0, 1597.0, 1597.0, 1597.0, 1597.0, 1597.0, 1595.0, 1595.0, 1595.0, 1607.0, 1607.0, 1607.0, 1599.0, 1593.0, 1601.0, 1597.0, 1601.0, 1610.0, 1612.0, 1615.0, 1617.0, 1609.0, 1609.0, 1609.0, 1620.0, 1626.0, 1631.0, 1631.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1635.0, 1640.0, 1640.0, 1640.0, 1630.0, 1635.0, 1637.0, 1638.0, 1640.0, 1630.0, 1636.0, 1644.0, 1652.0, 1655.0, 1648.0, 1656.0, 1656.0, 1670.0, 1675.0, 1672.0, 1672.0, 1679.0, 1681.0, 1681.0, 1681.0, 1683.0, 1685.0, 1694.0, 1681.0, 1681.0, 1681.0, 1687.0, 1673.0, 1684.0, 1697.0, 1697.0, 1702.0, 1701.0, 1703.0, 1702.0, 1705.0, 1706.0, 1715.0, 1718.0, 1713.0, 1716.0, 1723.0, 1723.0, 1724.0, 1724.0, 1727.0, 1729.0, 1729.0, 1739.0, 1739.0, 1734.0, 1734.0, 1736.0, 1741.0, 1741.0, 1743.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1745.0, 1753.0, 1751.0, 1756.0, 1763.0, 1764.0, 1765.0, 1765.0, 1767.0, 1768.0, 1769.0, 1775.0, 1775.0, 1777.0, 1783.0, 1785.0, 1790.0, 1790.0, 1795.0, 1789.0, 1791.0, 1792.0, 1795.0, 1799.0, 1800.0, 1805.0, 1804.0, 1804.0, 1806.0, 1812.0, 1815.0, 1812.0, 1818.0, 1821.0, 1825.0, 1830.0, 1834.0, 1836.0, 1838.0, 1838.0, 1842.0, 1842.0};

        double thesisExample[] = {1009.0, 1009.0, 1009.0, 1012.0, 1017.0, 1017.0, 1024.0, 1024.0, 1023.0, 1022.0};


        SimpleKalmanFilter filter = new SimpleKalmanFilter(SimpleKalmanFilter.FORWARD, 6, 1);
        double[] result = filter.smooth(thesisExample);

        /*SimpleKalmanFilter skf = new SimpleKalmanFilter(SimpleKalmanFilter.BACKWARD, 0.1, 0.05);
        double[] result = skf.smooth(data1);

        SimpleKalmanFilter skf2 = new SimpleKalmanFilter(SimpleKalmanFilter.FORWARD, 0.1, 0.05);
        double[] result2 = skf2.smooth(data1);

        for(int i = 0; i < data1.length; i++){
            System.out.println("ORIGINAL: " + data1[i] + ", ESTIMATE: " + (result[i] + result2[i]) / 2);
        }

        SimpleKalmanFilter skf3 = new SimpleKalmanFilter(SimpleKalmanFilter.COMBINED, 0.1, 0.05);
        double[] result3 = skf3.smooth(data1);

        for(int i = 0; i < data1.length; i++){
            System.out.println("ORIGINAL: " + data1[i] + ", ESTIMATE: " + result3[i]);
        }
        */

    }
}
