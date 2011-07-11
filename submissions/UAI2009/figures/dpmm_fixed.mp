input boxes
beginfig(1);

  verbatimtex
  \documentclass[10pt]{article}
  \usepackage{palatino}
  \usepackage{amsmath,amssymb,amsbsy,bbm}
  \begin{document}
  etex

  circleit.beta0(btex $\beta_0$ etex);
  circleit.u(btex $\boldsymbol{u}$ etex);
  circleit.beta1(btex $\beta_1$ etex);
  circleit.m(btex $\boldsymbol{n}$ etex);
  circleit.zeta(btex $\theta$ etex);
  circleit.mc(btex $G$ etex);
  circleit.mcd(btex $\boldsymbol{n}_d$ etex);
  circleit.phi(btex $\boldsymbol{\phi}_{d}$ etex);
  circleit.wtmp(btex $w_n$ etex);
  circleit.beta(btex $\beta$ etex);
  
  beta0.c-m.c = (75, 0);
  beta1.c-mc.c = (75, 0);
  beta.c-phi.c = (75, 0);
  u.c-m.c = (0, 50);
  m.c-mc.c = (0, 50);
  zeta.c-mc.c = (-75, 0);
  mc.c-mcd.c = (0, 50);
  mcd.c-phi.c = (0, 50);
  phi.c-wtmp.c = (0, 50);
  
  mc.e-mc.c = zeta.e-zeta.c = mcd.e-mcd.c = m.e-m.c = beta1.e-beta1.c = beta0.e-beta0.c = u.e-u.c = phi.e-phi.c = wtmp.e-wtmp.c = beta.e-beta.c = (12.5, 0);

  mc.n-mc.c = zeta.n-zeta.c = mcd.n-mcd.c = m.n-m.c = beta1.n-beta1.c = beta0.n-beta0.c = u.n-u.c = phi.n-phi.c = wtmp.n-wtmp.c = beta.n-beta.c = (0,12.5);

  fill bpath wtmp withcolor 0.8white;
  
  drawboxed(zeta,mc,mcd,m,beta1,u,beta0,phi,wtmp,beta);

  drawarrow u.s..m.n;
  drawarrow m.s..mc.n;
  drawarrow mc.s..mcd.n;
  drawarrow zeta.e..mc.w;
  drawarrow beta1.w..mc.e;
  drawarrow beta0.w..m.e;
  drawarrow mcd.s..phi.n;
  drawarrow phi.s..wtmp.n;
  drawarrow beta.w..phi.e;
  
%  boxit.dplate();
%  boxit.cplate();
  boxit.d2plate();
  boxit.nplate();

%  mc.s-dplate.n = (0, 12.5);
%  dplate.e-dplate.w = (75, 0);
%  dplate.n-dplate.s = (0, 50);

%  mc.n-cplate.n = (0, -12.5);
%  cplate.e-cplate.w = (100, 0);
%  cplate.n-cplate.s = (0, 112.5);

  mc.s-d2plate.n = (0, 12.5);
  d2plate.e-d2plate.w = (100, 0);
  d2plate.n-d2plate.s = (0, 162.5);

  phi.s-nplate.n = (0, 12.5);
  nplate.e-nplate.w = (75, 0);
  nplate.n-nplate.s = (0, 50);
  
%  drawboxed(dplate);
%  drawboxed(cplate);
  drawboxed(d2plate);
  drawboxed(nplate);

%  boxit.inf(btex $\infty$ etex);
%  boxit.D(btex $D$ etex);
  boxit.D2(btex $D$ etex);
  boxit.N(btex $N$ etex);

%  D.nw-dplate.nw = (1,-1);
%  inf.nw-cplate.nw = (1,-1);
  D2.nw-d2plate.nw = (1,-1);
  N.nw-nplate.nw = (1,-1);

%  drawunboxed(inf);
%  drawunboxed(D);
  drawunboxed(D2);
  drawunboxed(N);
  
endfig;
end;
