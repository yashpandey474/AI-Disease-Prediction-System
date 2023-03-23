/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.breastcancerprediction;

/**
 *
 * @author ohidu
 */
public class BreastCancerCompleteData {
    int codeNumber, clumpThickness, cSizeUni, cShapeUni, mAdhesion, secs, bareNuclei, 
            blandChromatin,  normalNucleoli, mitoses, classification;
    public BreastCancerCompleteData(int cn, int ct, int csu, int cshu, int ma,
                    int secs, int bn, int bc, int nn, int m, int cls){
        this.codeNumber = cn;
        this.clumpThickness = ct;
        this.cSizeUni = csu;
        this.cShapeUni = cshu;
        this.mAdhesion = ma;
        this.secs = secs;
        this.bareNuclei = bn;
        this.blandChromatin = bc;
        this.normalNucleoli = nn;
        this.mitoses = m;
        this.classification = cls;
    }
    public int getCodeNumber() {
        return codeNumber;
    }

    public void setCodeNumber(int codeNumber) {
        this.codeNumber = codeNumber;
    }

    public int getClumpThickness() {
        return clumpThickness;
    }

    public void setClumpThickness(int clumpThickness) {
        this.clumpThickness = clumpThickness;
    }

    public int getcSizeUni() {
        return cSizeUni;
    }

    public void setcSizeUni(int cSizeUni) {
        this.cSizeUni = cSizeUni;
    }

    public int getcShapeUni() {
        return cShapeUni;
    }

    public void setcShapeUni(int cShapeUni) {
        this.cShapeUni = cShapeUni;
    }

    public int getmAdhesion() {
        return mAdhesion;
    }

    public void setmAdhesion(int mAdhesion) {
        this.mAdhesion = mAdhesion;
    }

    public int getSecs() {
        return secs;
    }

    public void setSecs(int secs) {
        this.secs = secs;
    }

    public int getBareNuclei() {
        return bareNuclei;
    }

    public void setBareNuclei(int bareNuclei) {
        this.bareNuclei = bareNuclei;
    }

    public int getBlandChromatin() {
        return blandChromatin;
    }

    public void setBlandChromatin(int blandChromatin) {
        this.blandChromatin = blandChromatin;
    }

    public int getNormalNucleoli() {
        return normalNucleoli;
    }

    public void setNormalNucleoli(int normalNucleoli) {
        this.normalNucleoli = normalNucleoli;
    }

    public int getMitoses() {
        return mitoses;
    }

    public void setMitoses(int mitoses) {
        this.mitoses = mitoses;
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }
    
}
