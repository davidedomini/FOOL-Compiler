push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function1
lhp
sw
lhp
push 1
add
shp
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function2
lhp
sw
lhp
push 1
add
shp
push 1
push 1
lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 10000
push -4
add
lw
lhp
sw
lhp
lhp
push 1
add
shp
push 1
push 0
lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 10000
push -3
add
lw
lhp
sw
lhp
lhp
push 1
add
shp
push 1
push 1
beq label2
push 0
b label3
label2:
push 1
label3:
push 1
beq label0
lfp
push -6
add
lw
b label1
label0:
lfp
push -5
add
lw
label1:
print
halt

function0:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

function1:
cfp
lra
lfp
lw
push -2
add
lw
stm
sra
pop
sfp
ltm
lra
js

function2:
cfp
lra
lfp
lw
push -2
add
lw
stm
sra
pop
sfp
ltm
lra
js