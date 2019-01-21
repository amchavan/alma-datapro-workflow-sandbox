from setuptools import setup

dependencies = [
    "adapt-mock-icd",
    "adaptmb"
]

setup(name='adapt-mock-xtss',
      version='0.1',
      description='DRAWS Mock XTSS',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/xtss'],
      install_requires=dependencies,
      zip_safe=False)
