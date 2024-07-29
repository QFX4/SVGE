grammar Skeleton;

skeleton returns [ org.nxn.model.skeleton.ParsedJoint r ]:
    '[' j=joint ']' { $r = $j.r; } ;

joint returns [ org.nxn.model.skeleton.ParsedJoint r ]:
    nm=NAME ':' vec=vector3 ( ':' a=NAME)? (':' b=binding  )? ( ':' l=jointList )?
    { $r = new org.nxn.model.skeleton.ParsedJoint( $nm.text, $vec.r, $a.text, $b.r, $l.r); } ;

jointList returns [ org.nxn.model.skeleton.ParsedJoint[] r ]:
    { java.util.List<org.nxn.model.skeleton.ParsedJoint> l = new java.util.ArrayList<org.nxn.model.skeleton.ParsedJoint>(); }
    '[' j=joint { l.add($j.r); } (',' k=joint { l.add($k.r); } )* ']'
    { $r = l.toArray(new org.nxn.model.skeleton.ParsedJoint[0]); } ;

binding returns [ org.nxn.model.skeleton.ParsedBinding[] r ]:
    { java.util.List<org.nxn.model.skeleton.ParsedBinding> l = new java.util.ArrayList<org.nxn.model.skeleton.ParsedBinding>(); }
    '[' n=NAME ':' i=indList { l.add(new org.nxn.model.skeleton.ParsedBinding($n.text, $i.r)); }
        ( ',' m=NAME ':' j=indList { l.add(new org.nxn.model.skeleton.ParsedBinding($m.text, $j.r)); } )* ']'
    { $r = l.toArray( new org.nxn.model.skeleton.ParsedBinding[0] ); } ;

indList returns [ int[] r ]:
    { java.util.ArrayList<Integer> l = new java.util.ArrayList<Integer>(); }
    '[' n=indNum { l.add($n.r); } ( ',' m=indNum { l.add($m.r); } )* ']'
    { $r = l.stream().mapToInt(Integer::intValue).toArray(); };

vector3  returns [ org.nxn.math.Vector3f r ]:
    '(' a=floatNum ',' b=floatNum ',' c=floatNum ')'
    { $r = new org.nxn.math.Vector3f($a.r, $b.r, $c.r); };

floatNum returns [ float r ]:
    s=('+'|'-')? n=DIGITS ('.' m=DIGITS)? (('e'|'E') e=('+'|'-')? p=DIGITS )?
    {
        StringBuilder sb = new StringBuilder();
        if($s.text != null){
            sb.append($s.text);
        }
        sb.append($n.text);
        if($m.text != null){
            sb.append('.').append($m.text);
        }
        if($p.text != null){
            sb.append('E');
            if($e.text != null){
                sb.append($e.text);
            }
            sb.append($p.text);
        }
        $r = Float.parseFloat(sb.toString());
    };

indNum returns [ int r ]:
    n=DIGITS
    { $r = Integer.parseInt($n.text); } ;

NAME : ('a'..'z' | 'A'..'Z')('a'..'z' | 'A'..'Z' | '0'..'9' | '_')*;

DIGITS : ('0'..'9')+;

WS : (' '|'\t'|'\n'|'\r')+ -> skip ;
