package com.bhf.iex;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.HdrHistogram.Histogram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import pl.zankowski.iextrading4j.hist.api.IEXMessageType;
import pl.zankowski.iextrading4j.hist.api.message.IEXMessage;
import pl.zankowski.iextrading4j.hist.api.message.IEXSegment;
import pl.zankowski.iextrading4j.hist.deep.IEXDEEPMessageBlock;

public class IEXMessageDistroBuilder {

    public static void main(String[] args) throws PcapNativeException, InterruptedException, NotOpenException {
        final IEXMessageDistroBuilder deepSample = new IEXMessageDistroBuilder();
        deepSample.readDEEPsample(args[0]);
    }

    Histogram buyUpdatesHisto = new Histogram(10000, 1);
    Histogram sellUpdatesHisto = new Histogram(10000, 1);
    Histogram quoteUpdatesHisto = new Histogram(10000, 1);
    Histogram allUpdatesHisto = new Histogram(10000, 1);
    Histogram levelUpdatesHisto = new Histogram(10000, 1);

    private void readDEEPsample(String pcapFile) throws PcapNativeException {
        final PcapHandle handle = Pcaps.openOffline(
                pcapFile, PcapHandle.TimestampPrecision.MICRO);

        try {
            handle.loop(-1, new PacketListener() {
                int handled = 0;

                @Override
                public void gotPacket(final Packet packet) {
                    handled++;
                    if (handled % 1000000 == 0) {
                        System.out.println("Processed:" + handled);
                    }

                    final byte[] data =
                            packet.getPayload().getPayload().getPayload().getRawData();
                    final IEXSegment block = IEXDEEPMessageBlock.createIEXSegment(data);

                    int buyUpdates = 0;
                    int sellUpdates = 0;
                    int quoteUpdates = 0;

                    for (IEXMessage m : block.getMessages()) {
                        if (m.getMessageType() == IEXMessageType.PRICE_LEVEL_UPDATE_BUY) {
                            buyUpdates++;
                        } else if (m.getMessageType() == IEXMessageType.PRICE_LEVEL_UPDATE_SELL) {
                            sellUpdates++;
                        } else if (m.getMessageType() == IEXMessageType.QUOTE_UPDATE) {
                            quoteUpdates++;
                        }
                    }

                    buyUpdatesHisto.recordValue(buyUpdates);
                    sellUpdatesHisto.recordValue(sellUpdates);
                    quoteUpdatesHisto.recordValue(quoteUpdates);
                    int sum = buyUpdates + sellUpdates + quoteUpdates;
                    allUpdatesHisto.recordValue(sum);
                    int lvlUpdates = buyUpdates + sellUpdates;
                    levelUpdatesHisto.recordValue(lvlUpdates);
                }
            });
        } catch (PcapNativeException e) {

            e.printStackTrace();
        } catch (InterruptedException e) {

            e.printStackTrace();
        } catch (NotOpenException e) {

            e.printStackTrace();
        }

        handle.close();

        System.out.println("Outputting histograms");
        outputHisto();

        System.out.println("Finished outputting histograms");
    }

    private void outputHisto() {

        try {
            buyUpdatesHisto.outputPercentileDistribution(new PrintStream("BuyUpdatesHisto.txt"), 1.0);
            sellUpdatesHisto.outputPercentileDistribution(new PrintStream("SellUpdatesHisto.txt"), 1.0);
            quoteUpdatesHisto.outputPercentileDistribution(new PrintStream("QuoteUpdatesHisto.txt"), 1.0);
            allUpdatesHisto.outputPercentileDistribution(new PrintStream("AllUpdatesHisto.txt"), 1.0);
            levelUpdatesHisto.outputPercentileDistribution(new PrintStream("LevelUpdatesHisto.txt"), 1.0);
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }
    }
}
