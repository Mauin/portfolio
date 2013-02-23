package name.abuchen.portfolio.snapshot;

import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

import org.joda.time.DateMidnight;
import org.joda.time.Days;

public class SecurityIndex extends PerformanceIndex
{
    public static SecurityIndex forClient(ClientIndex clientIndex, Security security, List<Exception> warnings)
    {
        SecurityIndex index = new SecurityIndex(clientIndex.getClient(), clientIndex.getReportInterval());
        index.calculate(clientIndex, security, warnings);
        return index;
    }

    public SecurityIndex(Client client, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);

        dates = new Date[0];
        delta = new double[0];
        accumulated = new double[0];
        transferals = new long[0];
        totals = new long[0];
    }

    private void calculate(ClientIndex clientIndex, Security security, List<Exception> warnings)
    {
        List<SecurityPrice> prices = security.getPrices();
        if (prices.isEmpty())
            return;

        DateMidnight firstPricePoint = new DateMidnight(prices.get(0).getTime());
        if (firstPricePoint.isAfter(clientIndex.getReportInterval().getEndDate().getTime()))
            return;

        DateMidnight startDate = clientIndex.getFirstDataPoint().toDateMidnight();
        if (firstPricePoint.isAfter(startDate))
            startDate = firstPricePoint;

        DateMidnight endDate = new DateMidnight(clientIndex.getReportInterval().getEndDate());
        DateMidnight lastPricePoint = new DateMidnight(prices.get(prices.size() - 1).getTime());

        if (lastPricePoint.isBefore(endDate))
            endDate = lastPricePoint;

        int size = Days.daysBetween(startDate, endDate).getDays() + 1;

        dates = new Date[size];
        delta = new double[size];
        accumulated = new double[size];
        transferals = new long[size];
        totals = new long[size];

        final double adjustment = clientIndex.getAccumulatedPercentage()[Days.daysBetween(
                        new DateMidnight(clientIndex.getReportInterval().getStartDate()), startDate).getDays()];

        // first value = reference value
        dates[0] = startDate.toDate();
        delta[0] = 0;
        accumulated[0] = adjustment;
        long valuation = security.getSecurityPrice(startDate.toDate()).getValue();

        // calculate series
        int index = 1;
        DateMidnight date = startDate.plusDays(1);
        while (date.compareTo(endDate) <= 0)
        {
            dates[index] = date.toDate();

            long thisValuation = security.getSecurityPrice(date.toDate()).getValue();
            long thisDelta = thisValuation - valuation;

            delta[index] = (double) thisDelta / (double) valuation;
            accumulated[index] = ((accumulated[index - 1] + 1 - adjustment) * (delta[index] + 1)) - 1 + adjustment;

            date = date.plusDays(1);
            valuation = thisValuation;
            index++;
        }
    }
}
