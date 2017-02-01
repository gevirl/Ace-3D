/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Gamma;
import static org.rhwlab.BHC.NodeBase.beta;
import static org.rhwlab.BHC.NodeBase.maxN;
import org.rhwlab.dispim.datasource.MicroCluster;



/**
 *
 * @author gevirl
 */
public class LogNode extends NodeBase implements Node  {
    public LogNode(){
    }
    public LogNode(MicroCluster micro){  // node composed of a single microcluster
        super(micro);
        lnd = lnAlpha;
        lnPi = 0.0;
        lnR = 0.0;
        logPosterior();
    }
    public LogNode(LogNode l,LogNode r){
        super(l,r);
        logPosterior();
    }
    public LogNode(LogNode l,LogNode r,boolean post){
        super(l,r);
        if (post) logPosterior();
    }
   
    @Override
    public double  getLogPosterior() {
        return lnR;
    }    
    @Override
    public Node mergeWith(Node other) {
        return new LogNode(this,(LogNode)other);
    }    
    public double alternative(List<RealVector> data){
        int n = data.size();
        double nP = n + beta;
        if (n >maxN){
            return 0.0;
        }
        int D = data.get(0).getDimension(); 
        RealVector xSum = new ArrayRealVector(D);  // a vector of zeros  
        RealMatrix XXT = new Array2DRowRealMatrix(D,D);
        for (RealVector v : data){
            xSum = xSum.add(v);
            RealMatrix v2 = v.outerProduct(v);
            XXT = XXT.add(v2);
        }   
        RealMatrix Sp = S.add(XXT);
        Sp = Sp.add(m.outerProduct(m).scalarMultiply(beta*n/(nP)));
        Sp = Sp.add(xSum.outerProduct(xSum).scalarMultiply(1.0/(nP)));
        Sp = Sp.subtract(m.outerProduct(xSum).add(xSum.outerProduct(m)).scalarMultiply(beta/(nP)));
        
        LUDecomposition ed = new LUDecomposition(Sp);
        if (!ed.getSolver().isNonSingular()){
            System.exit(10);
        }
        return Utils.eln(ed.getDeterminant()); 

    }
    // use all the vioxels to compute the likelihood
    public double logLikelihood(){
        List<MicroCluster> micros = new ArrayList<>();
        this.getDataAsMicroCluster(micros);
        ArrayList<RealVector> data = new ArrayList<>();
        for (MicroCluster micro : micros){
            for (int i=0 ; i<micro.getPointCount() ; ++i){
                data.add(micro.getPoint(i));
            }
        }
        return logMarginalLikelihood(data);
    }
    // the likelihood of the data in this node/cluster only - given the priors
    public double logMarginalLikelihood(List<RealVector> data)  {

        int n = data.size();
        if (n >maxN){
            return 0.0;
        }
        int D = data.get(0).getDimension();
        double rP = beta + n;
        Double lnrP = Utils.eln(rP);
        double nuP = nu + n;       
        
        RealMatrix C = new Array2DRowRealMatrix(D,D);
        RealVector X = new ArrayRealVector(D);  // a vector of zeros    
        for (RealVector v : data){
            X = X.add(v);
            RealMatrix v2 = v.outerProduct(v);
            C = C.add(v2);
        }  
        
        RealVector mP = (m.mapMultiply(beta).add(X)).mapDivide(rP);
        RealMatrix Sp = C.add(S);
        
        RealMatrix rmmP = mP.outerProduct(mP).scalarMultiply(rP);
        Sp = Sp.add(rmm).subtract(rmmP);  
        
        LUDecomposition ed = new LUDecomposition(Sp);
        if (!ed.getSolver().isNonSingular()){
            System.exit(10);
        }
        double logDetSp = Utils.eln(ed.getDeterminant());
 //       double logDetSp = alternative(data);
        
        Double ret = Utils.elnPow(logPi, -n*D/2.0);
        ret = Utils.elnMult(ret, Utils.elnPow(lnBeta, D/2.0));
        ret = Utils.elnMult(ret,-Utils.elnPow(lnrP, D/2.0));
        ret = Utils.elnMult(ret, logdetSnu);
        ret = Utils.elnMult(ret, -Utils.elnPow(logDetSp, nuP/2.0));
        for (int i=1 ; i<=D ; ++i){
            ret = Utils.elnMult(ret, Gamma.logGamma((nuP+1-i)/2.0));
            ret = Utils.elnMult(ret,-Gamma.logGamma((nu+1-i)/2.0));
        }
        if (ThreadedAlgorithm.time < 175){
            ret = ret + data.size()*logRSDLikelihood();
        }
        return ret;
    }
    public double logDPMLikelihood(int n)throws ArithmeticException {       
        LogNode stdLeft = (LogNode)left;
        LogNode stdRight = (LogNode)right;
        if (left != null && right != null){
            lngn = Gamma.logGamma(n);
            
            if (!Double.isFinite(lngn))            {
                System.exit(111);
            }
            Double lnagn = Utils.elnMult(lnAlpha, lngn);
            if (!Double.isFinite(lnagn))            {
                System.exit(112);
            }  
            double lndd = Utils.elnMult(stdLeft.lnd, stdRight.lnd);
            lnd = Utils.elnsum(lnagn, Utils.elnMult(stdLeft.lnd,stdRight.lnd));
            if (lnd == 0.0){
                int aisohdfni=0;
            }
            if (!Double.isFinite(lnd))            {
                System.exit(222);
            }      
            
            lnPi = Utils.elnMult(lnagn,-lnd);
            if (lnPi == 0.0){
                int isafduis=0;
                lnd = Utils.elnsum(lnagn, Utils.elnMult(stdLeft.lnd,stdRight.lnd));
            }
            if (!Double.isFinite(lnPi))            {
                System.exit(223);
            }            
            lnonePi = Utils.eln(1.0 - Math.exp(lnPi));
            
            Double lnFirst = Utils.elnMult(lnPi,lnLike);
            Double prod = Utils.elnMult(stdLeft.lnDPM, stdRight.lnDPM);
            Double lnSecond = Utils.elnMult(lnonePi,prod);

            return Utils.elnsum(lnFirst, lnSecond);
        }
        return lnLike;
    }
    public double logRSDLikelihood(){
        double rsd = this.getIntensityRSD();
        return lnLambda - lambda*rsd;
    }
    // calculate the posterior of a non terminal node
    public void  logPosterior() throws ArithmeticException {
        List<RealVector> data = new ArrayList<>();
        this.getDataAsRealVector(data); 
        
        this.lnLike = this.logMarginalLikelihood(data);
 //       this.lnLike = this.logLikelihood();
        lnDPM = this.logDPMLikelihood(data.size());
       
        lnR = Utils.elnMult(lnPi, lnLike);
        lnR = Utils.elnMult(lnR, -lnDPM);
        if (lnR == 0){
            int iousahdfui=0;
        }

        
//        dfpR = field.newDfp(realR);
        
    }   
    @Override
    public void print(PrintStream stream){
        List<RealVector> data = new ArrayList<>();
        this.getDataAsRealVector(data);
        stream.printf("Size=%d\n", data.size());
/*        
        for (RealVector vec : data){
            stream.print(vectorAsString(vec));
        }
        stream.println();
 */       
        stream.printf("lnd=%s\n", Double.toString(lnd));
        stream.printf("lnGammaN=%s\n", Double.toString(this.lngn));
        stream.printf("ln1MinusPi=%s\n", Double.toString(lnonePi));
        stream.printf("lnPi=%s\n", Double.toString(lnPi));
        stream.printf("lnLike=%s\n", Double.toString(lnLike));
        stream.printf("lnDPM=%s\n", Double.toString(lnDPM));
        stream.printf("lnR=%s\n", Double.toString(lnR));
        stream.printf("R=%s\n", Double.toString(Math.exp(lnR)));
    }  
    
    public String vectorAsString(RealVector v){
        boolean first = true;
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i=0 ; i<v.getDimension() ; ++i){
            
            if (!first){
                builder.append(",");
            }
            builder.append(String.format("%.2f",v.getEntry(i)));
            first = false;
        }
        builder.append(")");
        return builder.toString();
    }  
    Double lngn;
    Double lnd;
    Double lnPi;
    Double lnonePi;
    
    Double lnDPM;
//    Double lnR;                   
    
    static double lambda = 1.0;
    static double lnLambda = Math.log(lambda);

    
}
